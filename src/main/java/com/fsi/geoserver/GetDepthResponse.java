package com.fsi.geoserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

public class GetDepthResponse extends Response {

    public GetDepthResponse() {
        super(DepthInfo.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return "text/plain";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        DepthInfo resp = (DepthInfo) value;
        PrintWriter pw = new PrintWriter(output);
        pw.println(resp.exact);
        if(!Double.isNaN(resp.avg)) {
            pw.println(resp.avg);
            pw.println(resp.min);
            pw.println(resp.max);
        }
        pw.flush();
    }

}
