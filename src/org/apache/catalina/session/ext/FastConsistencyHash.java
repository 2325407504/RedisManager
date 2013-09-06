package org.apache.catalina.session.ext;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * һ���Թ�ϣʵ��
 * 
 * @author wjw
 * 
 */
public class FastConsistencyHash {
	//Բ���ڵ�
	private final TreeMap<Long, String> nodes;

	//��������ڵ���Ŀ
	private int VIRTUAL_NUM = 160;

	public FastConsistencyHash(List<String> shards) {
		super();
		this.nodes = new TreeMap<Long, String>();
		for (int i = 0; i < shards.size(); i++) {
			String shardNode = shards.get(i);
			for (int j = 0; j < VIRTUAL_NUM; j++) {
				nodes.put(hash("SHARD-" + i + "-NODE-" + j), shardNode);
			}
		}
	}

	/**
	 * ����key��hashֵȡ�÷������ڵ���Ϣ
	 * 
	 * @param hash
	 * @return
	 */
	public String getNode(String key) {
		long hashKey = hash(key);
		SortedMap<Long, String> tailMap = nodes.tailMap(hashKey);
		if (tailMap.isEmpty()) {
			hashKey = nodes.firstKey();
		} else {
			hashKey = tailMap.firstKey();
		}
		return nodes.get(hashKey);
	}

	/**
	 * ��ӡԲ���ڵ�����
	 */
	public void printMap() {
		System.out.println(nodes);
	}

	private long hash64A(ByteBuffer buf, int seed) {
		ByteOrder byteOrder = buf.order();
		buf.order(ByteOrder.LITTLE_ENDIAN);

		long m = 0xc6a4a7935bd1e995L;
		int r = 47;

		long h = seed ^ (buf.remaining() * m);

		long k;
		while (buf.remaining() >= 8) {
			k = buf.getLong();

			k *= m;
			k ^= k >>> r;
			k *= m;

			h ^= k;
			h *= m;
		}

		if (buf.remaining() > 0) {
			ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			// for big-endian version, do this first:
			// finish.position(8-buf.remaining());
			finish.put(buf).rewind();
			h ^= finish.getLong();
			h *= m;
		}

		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;

		buf.order(byteOrder);
		return h;
	}

	/**
	 * ����2^32�ѽڵ�ֲ���Բ�����档
	 * 
	 * @param digest
	 * @return
	 */
	public long hash(String key) {
		byte[] keyBytes = null;
		try {
			keyBytes = key.getBytes("UTF-8");
			return hash64A(ByteBuffer.wrap(keyBytes), 0x1234ABCD);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown string :" + key, e);
		}

	}

	public static void main(String[] args) {
		List<String> shards = new ArrayList<String>();
		shards.add("192.168.0.0-������0");
		shards.add("192.168.0.1-������1");
		shards.add("192.168.0.2-������2");
		shards.add("192.168.0.3-������3");
		shards.add("192.168.0.4-������4");
		FastConsistencyHash hash = new FastConsistencyHash(shards);
		//hash.printMap();

		//ѭ��50�Σ���Ϊ��ȡ50����������Ч������ȻҲ�����������κε�����������
		for (int i = 0; i < 50; i++) {
			System.out.println(i + "->" + hash.getNode("key" + i));
		}
	}

}
