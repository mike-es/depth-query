package com.fsi.geoserver;

import org.geoserver.ows.Request;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GetDepth extends Request {
    private static final GeometryFactory FACTORY = new GeometryFactory();
    private String coverage;
    private double lat=Double.NaN, lon=Double.NaN;
    private double error = Double.NaN;
    private String crs = "EPSG:4326";

    public GetDepth() {}

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public double getErrorRadius() {
        return error;
    }

    public void setErrorRadius(double error) {
        this.error = error;
    }

    public double getRadius() {
        return getErrorRadius();
    }

    public void setRadius(double error) {
        setErrorRadius(error);
    }

    public Point getPoint() {
        return FACTORY.createPoint(new Coordinate(lon, lat));
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getCrs() {
        return crs == null ? "ESPG:4326" : crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }
}
