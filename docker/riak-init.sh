#!/bin/sh
set -eu

ensure_bucket_type() {
    type="$1"
    properties="$2"
    if ! riak-admin bucket-type status "$type" >/dev/null 2>&1; then
        riak-admin bucket-type create "$type" "$properties"
    fi
    if ! riak-admin bucket-type status "$type" | grep -Fxq "$type is active"; then
        riak-admin bucket-type activate "$type"
    fi
}

ensure_bucket_type expiration_bucket '{"props":{"n_val":1,"allow_mult":false}}'
ensure_bucket_type counters '{"props":{"n_val":1,"datatype":"counter","allow_mult":true}}'
