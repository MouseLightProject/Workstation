package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import java.util.Date;
import org.janelia.it.workstation.cache.large_volume.CacheController;
import org.janelia.it.workstation.cache.large_volume.CacheFacadeI;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;

/*
 * Loader for large volume viewer format negotiated with Nathan Clack
 * March 21, 2013.
 * 512x512 tiles
 * Z-order octree folder layout
 * uncompressed tiff stack for each set of slices
 * named like "default.0.tif" for channel zero
 * 16-bit unsigned int
 * intensity range 0-65535
 */
public class BlockTiffOctreeLoadAdapter 
extends AbstractTextureLoadAdapter 
{
	private static final Logger log = LoggerFactory.getLogger(BlockTiffOctreeLoadAdapter.class);
    public static final String CHANNEL_0_STD_TIFF_NAME = "default.0.tif";
    
	// Metadata: file location required for local system as mount point.
	private File topFolder;
    // Metadata: file location required for remote system.
    private String remoteBasePath;
    
	public LoadTimer loadTimer = new LoadTimer();
    
	public BlockTiffOctreeLoadAdapter()
	{
		tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
		// Report performance statistics when program closes
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				loadTimer.report();
			}
		});
	}
	
    public void setRemoteBasePath(String basePath) {
        remoteBasePath = basePath;
    }
    
	public File getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(File topFolder) 
	throws DataSourceInitializeException
	{
		this.topFolder = topFolder;
        OctreeMetadataSniffer sniffer = new OctreeMetadataSniffer(topFolder, tileFormat);
        sniffer.setRemoteBasePath(remoteBasePath);
		sniffer.sniffMetadata(topFolder);
		// Don't launch pre-fetch yet.
		// That must occur AFTER volume initialized signal is sent.
	}
    
	@Override
	public TextureData2dGL loadToRam(TileIndex tileIndex)
			throws TileLoadError, MissingTileException 
	{
        return loadToRam(tileIndex, true);
	}

	public TextureData2dGL loadToRam(TileIndex tileIndex, boolean zOriginNegativeShift)
			throws TileLoadError, MissingTileException 
	{
		// Create a local load timer to measure timings just in this thread
		LoadTimer localLoadTimer = new LoadTimer();
		localLoadTimer.mark("starting slice load");
        final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, zOriginNegativeShift);        
        if (octreeFilePath == null) {
            return null;
        }
		// TODO - generalize to URL, if possible
		// (though TIFF requires seek, right?)
		// Compute octree path from Raveler-style tile indices
		File folder = new File(topFolder, octreeFilePath.toString());
        
		// TODO for debugging, show file name for X tiles
		// Compute local z slice
		int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
		int axisIx = tileIndex.getSliceAxis().index();
		int tileDepth = tileFormat.getTileSize()[axisIx];
		int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
		int relativeSlice = absoluteSlice % tileDepth;
		// Raveller y is flipped so flip when slicing in Y (right?)
		if (axisIx == 1)
			relativeSlice = tileDepth - relativeSlice - 1;
		
        // Calling this with "true" means I, the caller, accept that the array
        // returned may have one or more nulls in it.
		ImageDecoder[] decoders = createImageDecoders(folder, tileIndex.getSliceAxis(), true);
		
		// log.info(tileIndex + "" + folder + " : " + relativeSlice);
		TextureData2dGL result = loadSlice(relativeSlice, decoders);
		localLoadTimer.mark("finished slice load");

		loadTimer.putAll(localLoadTimer);
		return result;
	}

	public TextureData2dGL loadSlice(int relativeZ, ImageDecoder[] decoders) 
	throws TileLoadError 
    {
		int sc = tileFormat.getChannelCount();
		// 2 - decode image
		RenderedImage channels[] = new RenderedImage[sc];
        boolean emptyChannel = false;
        for (int c = 0; c < sc; ++c) {
            if (decoders[c] == null)
                emptyChannel = true;
        }
        if (emptyChannel) {
            return null;
        }
        else {
            for (int c = 0; c < sc; ++c) {
                try {
                    ImageDecoder decoder = decoders[c];
                    assert (relativeZ < decoder.getNumPages());
                    channels[c] = decoder.decodeAsRenderedImage(relativeZ);
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
                // localLoadTimer.mark("loaded slice, channel "+c);
            }
            // Combine channels into one image
            RenderedImage composite = channels[0];
            if (sc > 1) {
                try {
                ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
                for (int c = 0; c < sc; ++c) {
                    pb.addSource(channels[c]);
                }
                composite = JAI.create("bandmerge", pb);
                } catch (NoClassDefFoundError exc) {
                    exc.printStackTrace();
                    return null;
                }
                // localLoadTimer.mark("merged channels");
            }

            TextureData2dGL result = null;
            // My texture wrapper implementation
            TextureData2dGL tex = new TextureData2dGL();
            tex.loadRenderedImage(composite);
            result = tex;
            return result;
        }
	}

	// TODO - cache decoders if folder has not changed
	public ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis)
			throws MissingTileException, TileLoadError
	{
		return createImageDecoders(folder, axis, false);
	}
	
	public ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis, boolean acceptNullDecoders)
			throws MissingTileException, TileLoadError 
	{
        String tiffBase = OctreeMetadataSniffer.getTiffBase(axis);
		int sc = tileFormat.getChannelCount();
		ImageDecoder decoders[] = new ImageDecoder[sc];
        StringBuilder missingTiffs = new StringBuilder();
        StringBuilder requestedTiffs = new StringBuilder();
        CacheFacadeI cacheManager = CacheController.getInstance().getManager();
		for (int c = 0; c < sc; ++c) {
			File tiff = new File(folder, OctreeMetadataSniffer.getFilenameForChannel(tiffBase, c));
            if ( requestedTiffs.length() > 0 ) {
                requestedTiffs.append("; ");
            }
            requestedTiffs.append(tiff);
			if (! tiff.exists()) {
                if ( acceptNullDecoders ) {
                    if ( missingTiffs.length() > 0 ) {
                        missingTiffs.append(", ");
                    }
                    missingTiffs.append( tiff );
                }
                else {
    				throw new MissingTileException("Putative tiff file: " + tiff);
                }
            }
            else {
                try {
                    boolean useUrl = false;
                    if (useUrl) { // So SLOW
                        // test URL stream vs (seekable) file stream
                        URL url = tiff.toURI().toURL();
                        InputStream inputStream = url.openStream();
                        decoders[c] = ImageCodec.createImageDecoder("tiff", inputStream, null);
                    } else {
                        SeekableStream s = null;
                        if (cacheManager != null) {
                            Date startCachMgrFetch = new Date();
                            if (cacheManager.isReady(tiff)) {
                                s = cacheManager.get(tiff);
                                log.info("Time in cache-mgr-fetch is {}.", new Date().getTime() - startCachMgrFetch.getTime());
                            }
                            else {                                
                                s = new FileSeekableStream(tiff);
                                log.info("Bypassing cache for {}. Time {}.", tiff, new Date().getTime() - startCachMgrFetch.getTime());
                            }
                        }
                        else {
                            s = new FileSeekableStream(tiff);
                        }
                        // Can have s null, if its task was cancelled.
                        if (s != null) {                            
                            decoders[c] = ImageCodec.createImageDecoder("tiff", s, null);
                        }
                        else {
                            missingTiffs.append(tiff);
                        }
                    }
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
            }
		}
        if ( missingTiffs.length() > 0 ) {
            log.debug("All requested tiffs: " + requestedTiffs);
            log.debug( "Putative tiff file(s): " + missingTiffs + " not found.  Padding with zeros." );
        }
		return decoders;
	}

}
