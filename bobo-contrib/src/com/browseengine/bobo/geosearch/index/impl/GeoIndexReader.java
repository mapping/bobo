/**
 * 
 */
package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.GeoIndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

/**
 * @author Shane Detsch
 * @author Ken McCracken
 * @author Geoff Cooney
 *
 */
public class GeoIndexReader extends FilterIndexReader {
    
    private static final int DEFAULT_BUFFER_SIZE_PER_SEGMENT = 16*1024;
    
    private List<GeoSegmentReader> geoSegmentReaders;
    
    private List<GeoIndexReader> subGeoReaders;
    
    public GeoIndexReader(Directory directory, GeoSearchConfig geoSearchConfig) throws IOException {
        super(initReader(directory, geoSearchConfig));

        if (subGeoReaders == null) {
            subGeoReaders = buildSubReaders();
        }
        
        for (GeoIndexReader subGeoReader: subGeoReaders) {
            subGeoReader.setGeoSearchConfig(geoSearchConfig);
        }
        
        geoSegmentReaders = buildGeoSegmentReaders(geoSearchConfig);
        
    }
    
    private GeoIndexReader(IndexReader reader) {
        super(reader);
        
        if (subGeoReaders == null) {
            subGeoReaders = buildSubReaders();
        }
    }
    
    private void setGeoSearchConfig(GeoSearchConfig geoSearchConfig) throws IOException {
        for (GeoIndexReader subGeoReader: subGeoReaders) {
            subGeoReader.setGeoSearchConfig(geoSearchConfig);
        }
        
        buildGeoSegmentReaders(geoSearchConfig);
    }
    
    List<GeoIndexReader> buildSubReaders() {
        IndexReader[] baseReaders = super.getSequentialSubReaders();
        
        int numReaders = baseReaders == null ? 0 : baseReaders.length;
        
        List<GeoIndexReader> subGeoReaders = new ArrayList<GeoIndexReader>(numReaders);
        for (int i = 0; i < numReaders; i++) {
            GeoIndexReader subReader = new GeoIndexReader(baseReaders[i]);
            subGeoReaders.add(subReader);
        }
        
        return subGeoReaders;
    }
    
    private List<GeoSegmentReader> buildGeoSegmentReaders(GeoSearchConfig geoSearchConfig) throws IOException {
        geoSegmentReaders = new ArrayList<GeoSegmentReader>();
        if (subGeoReaders == null || subGeoReaders.size() == 0) {
            if (in instanceof SegmentReader) {
                SegmentReader segmentReader = (SegmentReader) in;
                int maxDoc = segmentReader.maxDoc();
                String segmentName = segmentReader.getSegmentName();
                String geoSegmentName = geoSearchConfig.getGeoFileName(segmentName);
                GeoSegmentReader geoSegmentReader = new GeoSegmentReader(directory(), 
                        geoSegmentName, maxDoc, DEFAULT_BUFFER_SIZE_PER_SEGMENT);
                geoSegmentReaders.add(geoSegmentReader);
            } 
        } else {
            for (GeoIndexReader subReader : subGeoReaders) {
                for (GeoSegmentReader geoSegmentReader : subReader.getGeoSegmentReaders()) {
                    geoSegmentReaders.add(geoSegmentReader);
                }
            }
        }
        
        return geoSegmentReaders;
    }
    
    private static IndexReader initReader(Directory directory, GeoSearchConfig geoSearchConfig) throws IOException {
        if (null == directory) {
            return null;
        }
        directory = GeoIndexWriter.buildGeoDirectory(directory, geoSearchConfig);
        IndexReader indexReader = IndexReader.open(directory, true);
        return indexReader;
        
    }
    
    public List<GeoSegmentReader> getGeoSegmentReaders() {
        return geoSegmentReaders;
    }
    
    @Override
    public IndexReader[] getSequentialSubReaders() {
        
        if (subGeoReaders == null) {
            subGeoReaders = buildSubReaders();
        }

        if (subGeoReaders.size() == 0) {
            return null;
        }
        
        IndexReader[] subReaders = new IndexReader[subGeoReaders.size()];
        for (int i = 0; i < subReaders.length; i++) {
            subReaders[i] = subGeoReaders.get(i);
        }
        
        return subReaders;
    }

    public List<GeoIndexReader> getSubGeoReaders() {
        return subGeoReaders;
    }

    public void setSubGeoReaders(List<GeoIndexReader> subGeoReaders) {
        this.subGeoReaders = subGeoReaders;
    }

    public void setGeoSegmentReaders(List<GeoSegmentReader> geoSegmentReaders) {
        this.geoSegmentReaders = geoSegmentReaders;
    }
}
