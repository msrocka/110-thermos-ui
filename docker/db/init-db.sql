-- set database user and password
GRANT ALL PRIVILEGES ON DATABASE thermos TO postgres;
ALTER USER postgres WITH PASSWORD 'therm0s';

-- enable PostGIS extensions
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_raster;
