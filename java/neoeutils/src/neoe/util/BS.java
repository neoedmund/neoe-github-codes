package neoe.util;

import java.nio.ByteBuffer;

public class BS {
	public BS(byte[] bs, int pos, int len) {
		this.bs = bs;
		this.pos = pos;
		this.len = len;
	}

	public byte[] bs;
	public int pos;
	public int len;

	public ByteBuffer toByteBuffer() {
		ByteBuffer bb = ByteBuffer.wrap(bs, pos, len);
		return bb;
	}
}
