package org.gotti.wurmunlimited.mods.vyspermod;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Hex;

public class IdSequence {
	
	private String base;
	private AtomicLong sequence;

	public IdSequence(String base) {
		byte bytes[] = new byte[4];
		new Random().nextBytes(bytes);
		this.base = base + Hex.encodeHexString(bytes);
		this.sequence = new AtomicLong();
	}

	public String getNextId() {
		return this.base + this.sequence.getAndIncrement();
	}
}
