package org.icpc.tools.cds.video.containers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.icpc.tools.cds.video.VideoStreamHandler.IStreamListener;

public class FLVReader {
	private byte[] metadata = null;
	private byte[] mpegData = null;

	public void read(DataInputStream din, IStreamListener listener) throws IOException {
		// read FLV
		din.readByte();
		din.readByte();
		din.readByte();
		// System.out.println(new String(new byte[] { f, l, v }));

		// read version byte
		// int version =
		din.readUnsignedByte();
		// System.out.println("Version: " + version);

		// read flags
		// int flags =
		din.readUnsignedByte();
		// System.out.println("Flags: " + flags);

		// header size
		readUInt32(din);

		while (!listener.isDone()) {
			// read last packet start
			// long last =
			readUInt32(din);

			int packetType = din.read();
			if (packetType == -1)
				break;

			FLVPacket p = new FLVPacket();
			p.type = packetType;

			long payloadSize = readUInt24(din);
			// System.out.println("IPacket: " + packetType + " " + payloadSize + " " + last);

			p.timestamp = readUInt24(din); // skip timestamps
			p.timestamp2 = din.readUnsignedByte();
			p.streamId = readUInt24(din); // skip streamID

			if (packetType == 18) { // metadata packet
				byte[] b = new byte[(int) payloadSize];
				din.readFully(b);
				p.payload = b;

				// System.out.println(din.read(b));
				// System.out.println(new String(b));
				readMetadata(b);
			} else if (packetType == 9) {
				byte[] b = new byte[(int) payloadSize];
				din.readFully(b);
				p.payload = b;

				// System.out.println(((b[0] >> 4) & 10) + " " + (b[0] & 10));
			} else {
				byte[] b = new byte[(int) payloadSize];
				din.readFully(b);
				p.payload = b;
				/*long skip = din.skip(payloadSize);
				if (skip != payloadSize) {
					System.err.println("Skip failure! " + skip + " " + payloadSize);
				}*/
				// else
				// System.out.println("Skipped: " + skip);
			}
			if (packetType != 8) {
				listener.write(p.payload);
			}
			if (p.type == 18)
				metadata = p.payload;
			if (p.type == 9 && mpegData == null)
				mpegData = p.payload;
		}

		// in.close();
	}

	protected void sendCache(OutputStream out) throws IOException {
		out.write(metadata);
		out.write(mpegData);
	}

	@SuppressWarnings("unchecked")
	private static void readMetadata(byte[] b) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(b);
		DataInputStream dis = new DataInputStream(is);

		Object data = null;

		for (int i = 0; i < 2; i++) {
			data = readAMFData(dis, -1);
			// System.out.println(data.getClass() + " " + data);
		}

		if (data instanceof Map<?, ?>) {
			// TODO if there are multiple metadata values with same key (in
			// separate AMF blocks, we currently loose previous values)
			Map<String, Object> extractedMetadata = (Map<String, Object>) data;
			for (Entry<String, Object> entry : extractedMetadata.entrySet()) {
				if (entry.getValue() == null) {
					continue;
				}
				// System.out.println(entry.getKey() + " - " + entry.getValue().toString());
				// metadata.set(entry.getKey(), entry.getValue().toString());
			}
		}
	}

	private static Object readAMFData(DataInputStream input, int type2) throws IOException {
		int type = type2;
		if (type == -1)
			type = input.readUnsignedByte();

		switch (type) {
			case 0:
				return input.readDouble();
			case 1:
				return input.readUnsignedByte() == 1;
			case 2:
				return readAMFString(input);
			case 3:
				return readAMFObject(input);
			case 8:
				return readAMFEcmaArray(input);
			case 10:
				return readAMFStrictArray(input);
			case 11:
				final Date date = new Date((long) input.readDouble());
				input.readShort(); // time zone
				return date;
			case 13:
				return "UNDEFINED";
			default:
				return null;
		}
	}

	private static Object readAMFStrictArray(DataInputStream input) throws IOException {
		long count = readUInt32(input);
		ArrayList<Object> list = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			list.add(readAMFData(input, -1));
		}
		return list;
	}

	private static String readAMFString(DataInputStream in) throws IOException {
		int size = in.readUnsignedShort();
		byte[] chars = new byte[size];
		in.readFully(chars);
		return new String(chars);
	}

	private static Object readAMFObject(DataInputStream in) throws IOException {
		HashMap<String, Object> array = new HashMap<>();
		while (true) {
			String key = readAMFString(in);
			int dataType = in.read();
			if (dataType == 9) { // object end marker
				break;
			}
			array.put(key, readAMFData(in, dataType));
		}
		return array;
	}

	private static Object readAMFEcmaArray(DataInputStream input) throws IOException {
		long size = readUInt32(input);
		HashMap<String, Object> array = new HashMap<>();
		for (int i = 0; i < size; i++) {
			String key = readAMFString(input);
			int dataType = input.read();
			array.put(key, readAMFData(input, dataType));
		}
		return array;
	}

	private static long readUInt32(DataInputStream din) throws IOException {
		return din.readInt() & 0xFFFFFFFFL;
	}

	private static int readUInt24(DataInputStream din) throws IOException {
		return (din.read() << 16) + (din.read() << 8) + din.read();
	}
}