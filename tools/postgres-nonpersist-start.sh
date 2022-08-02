#!/usr/bin/env bash

# Start a clean postgres instance on docker, this will delete older instances.
docker run --rm -p 5432:5432 --name postgresql -e POSTGRES_DB=cordacluster -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password postgres:latest
# docker run --rm -p 5432:5432 --name postgresql -e POSTGRES_DB=cordacluster -e POSTGRES_USER=cordappdev -e POSTGRES_PASSWORD=password postgres:latest

