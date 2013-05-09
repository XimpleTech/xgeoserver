package org.geoserver.catalog.rest;

import java.util.Collection;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.XMarkInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class XMarkListResource extends AbstractCatalogListResource {
    public XMarkListResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, XMarkInfo.class, catalog);
    }

    @Override
    protected Collection handleListGet() throws Exception {
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            LOGGER.fine( "GET xmarks for workspace " + workspace );
            return catalog.getXMarksByWorkspace(workspace);
        }
        LOGGER.fine( "GET xmarks" );

        return catalog.getXMarksByWorkspace(CatalogFacade.NO_WORKSPACE);
    }
}
