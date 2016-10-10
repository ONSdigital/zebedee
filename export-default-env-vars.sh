#!/bin/bash

echo 'setting default env vars...'

: ${db_audit_url='jdbc:postgresql://localhost:5432/audit'} # set the default connection url if its not already defined.
: ${db_audit_username='postgres'} # set the default connection url if its not already defined.
: ${db_audit_password='mysecretpassword'} # set the default connection url if its not already defined.

export db_audit_url
export db_audit_username
export db_audit_password