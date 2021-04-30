/* This file is part of 'agarnet'
 *
 * Copyright (c) Facebook, Inc. and its affiliates
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
 
#ifndef _FLAKEY_PROTO_H
#define _FLAKEY_PROTO_H

#include <stdint.h>
#include "agarnet.h"

void *flakeyproto_new (float fraction);
void flakeyproto_free (void *proto);

void flakeyproto_set_debug (void *proto, void (*debug) (void *));
void flakeyproto_get_debug_msg (void *proto, char *, size_t len);
void flakeyproto_setid (void *proto, long long id);
void flakeyproto_up (const void *proto, long long from, uint8_t *data, size_t len); 
void flakeyproto_down (const void *proto, long long to, uint8_t *data, size_t len);
void flakeyproto_set_send_notify (void *proto, void (*send_cb) (void *, long long, void *));
size_t flakeyproto_send_out (const void *proto, void *msgp, uint8_t *data, size_t len);
void flakeyproto_flood (void *proto, long long from, uint8_t *data, size_t len);
void *flakeyproto_link_add (void *p, long long neighbour);
void flakeyproto_link_remove (const void *p, long long neighbour);

#endif /* _FLAKEY_PROTO_H */
