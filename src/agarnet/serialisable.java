/* This file is part of 'agarnet'
 *
 * Copyright (C) 2013 Paul Jakma
 *
 * agarnet is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * agarnet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with agarnet.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet;

import java.io.IOException;

/**
 * Marker interface for a serialisation interface of objects.
 *
 * Similarish in spirit to the standard Java Serializable interface, however
 * this allows for a more direct and lower-overhead implementation,
 * serialising straight to byte[] and not having to rely on the standard
 * Serializable infrastructure - which has a degree of overhead, that is
 * noticeable in performance at scale.
 *
 * Given the near-duplication of name with Serializable, this interface
 * should generally by referred to by the fully qualified
 * 'agarnet.serialisable' and not imported.
 *
 * This interface expects that an implementing class T also implements a
 * static method:
 * 
 * 	static T deserialise (byte [])
 *
 * There is no reasonable way to specify this requirement in Java, in a
 * well-typed way, that I could find, other than with tricks with reflection
 * - which would defeat the performance motivation for this interface.
 */
public interface serialisable {
  public byte [] serialise () throws IOException;
}
