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

/**
 * Example native implementation of a protocol
 *
 * This 'flakey' protocol simply floods on messages with a given probability to
 * each neighbour.  It also fails to store state.
 */

#include "agarnet.h"

#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <stdarg.h>
#undef NDEBUG
#include <assert.h>
#include <string.h>

struct flakey_proto_data {
  long long id;
  long long maxneighbour;
  size_t len;
  long int rand_threshold;
  
  /* callback to notify for debug print event */
  void (*debug_notify) ();
  /* debug buffer */
  char debug_msg[256];

  /* cb to send msg */
  void (*send_notify) (void *, long long, void *);

  /* dynamic array-map of neighbour Id -> whether it is connected. 
   * XXX: demo purposes... obv does not scale.
   */
  bool neighbours[0];
};

#define PROTO_TABLE_INIT_SIZE 1000

static
size_t flakeyproto_tablesize (int maxneighbour) {
  return sizeof (struct flakey_proto_data)
         + ((maxneighbour + 1) * sizeof (bool));
}

static
void
flakeyproto_debug (struct flakey_proto_data *proto, const char *fmt, ...) {      
  if (!proto->debug_notify)
    return;
  
  va_list ap;

  va_start (ap, fmt);
  vsnprintf (proto->debug_msg, sizeof (proto->debug_msg), fmt, ap);
  va_end (ap);

  proto->debug_notify (proto);
}
#define _debug flakeyproto_debug

void flakeyproto_set_debug (void *p, void (*debug) (void *)) {
    struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
    proto->debug_notify = debug;
}

void flakeyproto_get_debug_msg (void *p, char *msg, size_t len) {
  struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
  size_t lim = sizeof (proto->debug_msg);  
  lim = (len < lim ? len : lim);
  strncpy (msg, proto->debug_msg, lim);
  msg[lim - 1] = '\0';
}

void *flakeyproto_new (float fraction) {
  /* no decent data lib in C... just a quick hack for example use */
  struct flakey_proto_data *proto
    = calloc (1, flakeyproto_tablesize (PROTO_TABLE_INIT_SIZE));
  proto->len = PROTO_TABLE_INIT_SIZE;
  proto->rand_threshold = (long int) (((float) RAND_MAX) * ((float) fraction));
  return proto;
}

void flakeyproto_free (void *proto) {
  free (proto);
}


void flakeyproto_setid (void *p, long long id) {
    struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
    proto->id = id;
    _debug (proto, "%s:%lu \n", __func__, id);
}

void flakeyproto_set_send_notify (void *p, 
  void (*send_cb) (void *, long long, void *)) {
  
  struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
  proto->send_notify = send_cb;
}

static const char *
bytes_to_str (char *str, size_t len, struct native_buffer *buf) {
  static const char *HEX_ARRAY = "0123456789ABCDEF";
  for (int i = 0, j = 0; i < buf->len && (j + 2) < len; i++, j += 3) {
    uint8_t v = buf->data[i] & 0xff;
    str[j] = ' ';
    str[j + 1] = HEX_ARRAY[v >> 4];
    str[j + 2] = HEX_ARRAY[v & 0xf];
  }
  
  if ((buf->len * 3) < len)
    str[buf->len * 3] = '\0';
  str[len - 1] = '\0';

  return str;
}

void flakeyproto_up (const void *p, long long from, uint8_t *data, size_t len) {
    struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
    struct native_buffer *buf = &(struct native_buffer) {
       .data = data, .len = len, .capacity = len
    };
    char strbuf[200];
    _debug (proto, "%s:%lu: from %llu, %zu bytes\n",
            __func__, proto->id, from, len);
    //_debug (proto, "%s:%lu: msg bytes %s xx\n", __func__, proto->id, 
    //        bytes_to_str (strbuf, sizeof (strbuf), buf));

    if (!proto->send_notify)
      return;
    
    for (int i = 0; i <= proto->maxneighbour; i++) {
      if (proto->neighbours[i] == false)
        continue;
    
      _debug (proto, "%s:%lu: consider send to %llu\n",
              __func__, proto->id, i);
      
      if (random () > proto->rand_threshold)
        continue;

      _debug (proto, "%s:%lu: send to %llu\n",
              __func__, proto->id, i);
    
      proto->send_notify (proto, i, buf);
    }
}

size_t flakeyproto_send_out (const void *p, void *msgp,
                             uint8_t *data, size_t len) {
  struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
  struct native_buffer *buf = (struct native_buffer *) msgp;
  
  size_t cpylen = len < buf->len ? len : buf->len;
  memcpy (data, buf->data, cpylen);
    
  _debug (proto, "%s:%lu: convert message %lu msg to %lu size buffer\n",
          __func__, proto->id, len, cpylen);
  return cpylen;
}

void flakeyproto_down (const void *p, long long to, uint8_t *data, size_t len) {
    struct native_buffer *buf = &(struct native_buffer) {
      .data = data, .len = len, .capacity = len
    };
    struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
    _debug (proto, "%s:%lu: to %lld, %zu bytes, byte 1 %d\n",
            __func__, proto->id, to, buf->len, buf->data[0]);
}

void *flakeyproto_link_add (void *p, long long neighbour) {
  struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
  _debug (proto, "%s:%lu: neighbour %lu\n", __func__, proto->id, neighbour);
  
  if (neighbour > proto->maxneighbour)
    proto->maxneighbour = neighbour;
  if (proto->len < proto->maxneighbour + 1) {
    size_t newsize = flakeyproto_tablesize (neighbour);
    struct flakey_proto_data *newproto = calloc (1, newsize);
    _debug (proto, "%s: Resizing to %u neighbours, size %zu to %zu\n",
            __func__, proto->maxneighbour, proto->len, newsize);
    memcpy (newproto, proto, flakeyproto_tablesize (proto->len));
    free (proto);
    proto = newproto;
    proto->len = newsize;
  }
  
  proto->neighbours[neighbour] = true;
  
  return proto;
}

void flakeyproto_link_remove (const void *p, long long neighbour) {
  struct flakey_proto_data *proto = (struct flakey_proto_data *) p;
  assert (neighbour <= proto->maxneighbour);
  _debug (proto, "%s:%lu: neighbour %lu\n", __func__, proto->len, neighbour);

  proto->neighbours[neighbour] = false;
}
