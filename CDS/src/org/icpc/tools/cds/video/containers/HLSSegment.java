package org.icpc.tools.cds.video.containers;

/**
 * An HLS media segment.
 */
public class HLSSegment {
	protected String[] comments;
	protected String[] parts;
	protected String file;
	protected boolean gap;

	// HLS byte range: [ length, start ]
	protected int[] byterange;
}