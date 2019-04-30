package org.icpc.tools.cds.video.containers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class FLVWriter {
	private long last = -1;

	public static void write(DataOutputStream dout, List<FLVPacket> list) throws IOException {
		// DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));

		// write FLV
		dout.writeByte('F');
		dout.writeByte('L');
		dout.writeByte('V');

		// write version byte
		dout.writeByte(1);

		// write flags
		// dout.writeByte(5);
		dout.writeByte(1);

		// header size
		writeUInt32(dout, 9);

		// writeMetadata(dout);

		long last = -1;

		for (FLVPacket p : list) {
			if (last == -1) {
				writeUInt32(dout, 0);
				/*dout.writeByte(0);
				dout.writeByte(0);
				dout.writeByte(0);
				dout.writeByte(0);*/
			} else
				writeUInt32(dout, (dout.size() - last));
			last = dout.size();

			dout.writeByte(p.type);

			writeUInt24(dout, p.payload.length);

			// writeUInt32(dout, 0); // timestamp
			writeUInt24(dout, p.timestamp);
			dout.writeByte(p.timestamp2);
			writeUInt24(dout, p.streamId);

			dout.write(p.payload);
		}

		// writeUInt32(dout, 0);
		writeUInt32(dout, (dout.size() - last));
		// dout.writeByte(-1);

		dout.close();
	}

	public static void writeHeader(DataOutputStream dout) throws IOException {
		// write FLV
		dout.writeByte('F');
		dout.writeByte('L');
		dout.writeByte('V');

		// write version byte
		dout.writeByte(1);

		// write flags
		// dout.writeByte(5);
		dout.writeByte(1);

		// header size
		writeUInt32(dout, 9);
	}

	public void writePacket(DataOutputStream dout, FLVPacket p) throws IOException {
		long last2 = -1;

		if (last2 == -1) {
			writeUInt32(dout, 0);
			/*dout.writeByte(0);
			dout.writeByte(0);
			dout.writeByte(0);
			dout.writeByte(0);*/
		} else
			writeUInt32(dout, (dout.size() - last2));
		last2 = dout.size();

		dout.writeByte(p.type);

		writeUInt24(dout, p.payload.length);

		// writeUInt32(dout, 0); // timestamp
		writeUInt24(dout, p.timestamp);
		dout.writeByte(p.timestamp2);
		writeUInt24(dout, p.streamId);

		dout.write(p.payload);
		dout.flush();
	}

	public void writeFooter(DataOutputStream dout) throws IOException {
		writeUInt32(dout, (dout.size() - last));
	}

	protected static void writeMetadata(DataOutputStream dout) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		DataOutputStream dos = new DataOutputStream(os);

		// AMF header
		writeAMFString(dos, "onMetadata");

		HashMap<String, Object> map = new HashMap<>();
		map.put("audiocodecid", 2.0);
		map.put("audiosamplerate", 44100.0);
		map.put("videodatarate", 0.0);
		map.put("videocodecid", 7.0);
		map.put("filesize", 0.0);
		map.put("audiodatarate", 125.0);
		map.put("audiosamplesize", 16.0);
		map.put("framerate", 29.97008985032937);
		map.put("encoder", "Lavf55.19.104");
		map.put("width", 1280.0);
		map.put("height", 720.0);
		map.put("duration", 0.0);
		writeAMFObject(dos, map);

		byte[] b = os.toByteArray();

		writeUInt32(dout, 0);

		dout.writeByte(18);

		writeUInt24(dout, b.length);

		writeUInt32(dout, 0); // timestamp
		writeUInt24(dout, 0);

		dout.write(b);
	}

	private static void writeAMFString(DataOutputStream out, String s) throws IOException {
		byte[] chars = s.getBytes();
		out.writeShort(chars.length);
		out.write(chars);
	}

	private static void writeAMFObject(DataOutputStream out, HashMap<String, Object> array) throws IOException {
		for (String s : array.keySet()) {
			writeAMFString(out, s);
			Object o = array.get(s);
			if (o instanceof Double) {
				out.write(0);
				out.writeDouble((Double) o);
			} else if (o instanceof Boolean) {
				out.write(1);
				writeAMFString(out, (String) o);
			} else if (o instanceof String) {
				out.write(2);
				out.write(0); // TODO
			} else
				throw new IOException("Unknown object in AMF Object");
		}
		out.writeByte(9);
	}

	private static void writeUInt32(DataOutputStream dout, long l) throws IOException {
		dout.writeInt((int) (l & 0xFFFFFFFFL)); // TODO likely wrong
	}

	private static void writeUInt24(DataOutputStream dout, int i) throws IOException {
		dout.writeByte(i >> 16);
		dout.writeByte(i >> 8);
		dout.writeByte(i);
		// TODO likely wrong
	}
}