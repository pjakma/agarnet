/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010 Paul Jakma
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
package agarnet.protocols;

public interface protocol_stats {
  void stats_reset ();
  void stats_reset (stat s);
  
  /* statistics */
  enum stat {
    /**
     * Number of messages sent down by this protocol
     */
    sent,
    /**
     * Number of messages sent down by this protocol
     */
    recvd,
    /**
     * Number of incoming messages dropped by this protocol
     */
    dropped,
    /**
     * An estimate of the state held by this protocol. Exactly what the
     * dimension is of this value is a matter for the protocol.
     */
    stored;
  };
  long stat_get (stat s);
  long stat_get (int ordinal);
}
