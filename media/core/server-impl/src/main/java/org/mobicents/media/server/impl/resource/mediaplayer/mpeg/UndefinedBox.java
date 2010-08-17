/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.resource.mediaplayer.mpeg;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * We don't know the type of box but we keep content as byte[]
 * 
 * @author amit bhayani
 * 
 */
public class UndefinedBox extends Box {

	public UndefinedBox(long size, String type) {
		super(size, type);
	}

	byte[] rawData = null;

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int length = (int) (this.getSize() - 8);
		rawData = new byte[length];
		fin.read(rawData, 0, length);
		return (int) this.getSize();
	}

}