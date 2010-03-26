package com.fsi.geoserver;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DepthService implements ApplicationContextAware {
    final Logger LOG;
    ApplicationContext ctx;
    final GeoServer gs;
    final Catalog info;
    final double lowerSampleLimit, upperSampleLimit;

    public DepthService(GeoServer gs) {
        this(gs, Double.NaN, Double.NaN);
    }

    public DepthService(GeoServer gs, double lowerSampleLimit,
        double upperSampleLimit) {
        this.gs = gs;
        info = gs.getCatalog();
        LOG = Logging.getLogger(DepthService.class);
        this.lowerSampleLimit = lowerSampleLimit;
        this.upperSampleLimit = upperSampleLimit;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext;
    }

    protected CoverageInfo getCoverage(GetDepth request) {
        String name = request.getCoverage();

        {
            int colon = name.indexOf(':');
            if(colon > 0) {
                String pref = name.substring(0, colon);
                name = name.substring(colon + 1);
                NamespaceInfo ns = info.getNamespaceByPrefix(pref);
                return info.getCoverageByName(ns, pref);
            }
        }

        return info.getCoverageByName(name);
    }

    public DepthInfo getDepth(GetDepth request) throws IOException,
        TransformException, ServiceException {

        if(Double.isNaN(request.getLat()) || Double.isNaN(request.getLon()))
            throw new ServiceException("'lat' and 'lon' are required parameters", "InvalidParameterValue");
        if(request.getCoverage() == null)
            throw new ServiceException("'coverage' is a required parameter", "InvalidParameterValue");

        CoverageInfo cinfo = getCoverage(request);
        if(cinfo == null)
            throw new ServiceException("Coverage not found: " +
                request.getCoverage(), "InvalidParameterValue");
        if(cinfo.getDimensions().size() != 1)
            throw new ServiceException(
                "Only one-dimensional coverages are allowed. The coverage you have chosen has " +
                    cinfo.getDimensions().size() + " dimensions.",
                "InvalidParameterValue");

        CoordinateReferenceSystem crs;

        try {
            crs = CRS.decode(request.getCrs());
        } catch(NoSuchAuthorityCodeException e) {
            throw new ServiceException("CRS could not be parsed: " +
                request.getCrs(), e, "InvalidParameterValue");
        } catch(FactoryException e) {
            throw new ServiceException("CRS could not be instantiated: " +
                request.getCrs());
        }

        DepthInfo depthInfo = new DepthInfo();
        depthInfo.request = request;

        com.vividsolutions.jts.geom.Point point = request.getPoint();
        GridCoverage2D coverage =
            (GridCoverage2D) cinfo.getGridCoverage(null, null);

        try {

            {
                double x = point.getX();
                double y = point.getY();
                DirectPosition2D pos = new DirectPosition2D(crs, x, y);
                GridCoordinates2D gridPos =
                    coverage.getGridGeometry().worldToGrid(pos);
                double[] sample = new double[1];
                coverage.evaluate(gridPos, sample);

                if(sample[0] < lowerSampleLimit || sample[0] > upperSampleLimit) {}
                else {
                    depthInfo.exact = sample[0];
                }
            }

            if(!Double.isNaN(request.getErrorRadius())) {
                Envelope env;

                {
                    final double equator = 40075016.686D;
                    double buffer = request.getErrorRadius() * 360 / equator;
                    double x = point.getX();
                    double y = point.getY();
                    env =
                        new ReferencedEnvelope(x - buffer, x + buffer, y -
                            buffer, y + buffer, crs);
                    env = CRS.transform(env, cinfo.getCRS());
                }

                Envelope2D env2d = new Envelope2D(env);
                GridEnvelope2D genv2d =
                    coverage.getGridGeometry().worldToGrid(env2d);

                int n = 0;
                double min, max, tot;
                double sample[] = new double[1];
                GridCoordinates2D dp2d = new GridCoordinates2D();

                min = max = Double.NaN;
                tot = 0;

                GridEnvelope2D range =
                    coverage.getGridGeometry().getGridRange2D();

                for(int y = 0; y < genv2d.height; ++y) {
                    dp2d.y = genv2d.y + y;
                    int w =
                        (int) (Math.sin(Math.PI * y / genv2d.height) * genv2d.width);

                    if(w < 1) w = 1;
                    for(int x = (int) (genv2d.getCenterX() - w / 2); w > 0; ++x, --w) {
                        dp2d.x = x;
                        if(!range.contains(dp2d)) continue;
                        coverage.evaluate(dp2d, sample);
                        if(sample[0] < lowerSampleLimit ||
                            sample[0] > upperSampleLimit) continue;

                        if(!(min < sample[0])) min = sample[0];
                        if(!(max > sample[0])) max = sample[0];
                        tot += sample[0];
                        ++n;
                    }
                }

                depthInfo.max = max;
                depthInfo.min = min;
                depthInfo.avg = tot / n;
            }

            return depthInfo;
        } finally {
            // coverage.dispose(false);
        }
    }
}
