FROM postgres:latest

# Install PostGIS
RUN apt-get update && \
    apt-get install -y postgis postgresql-13-postgis-3

# Copy initialization scripts
COPY init-db.sql /docker-entrypoint-initdb.d/

# Set environment variables
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=therm0s
ENV POSTGRES_DB=thermos

EXPOSE 5432
