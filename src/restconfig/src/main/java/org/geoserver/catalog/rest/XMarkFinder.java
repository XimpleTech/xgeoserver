package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class XMarkFinder extends AbstractCatalogFinder {
    public XMarkFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String workspace = getAttribute(request, "workspace");
        String xmark = getAttribute(request, "xmark");

        //check if workspace exists
        if (workspace != null && catalog.getWorkspaceByName(workspace) == null) {
            throw new RestletException( "No such workspace: " + workspace, Status.CLIENT_ERROR_NOT_FOUND );
        }
        //check xmark exists if specified
        if ( xmark != null) {
            if (workspace != null && catalog.getXMarkByName( workspace, xmark ) == null) {
                throw new RestletException(String.format("No such xmark %s in workspace %s",
                    xmark, workspace), Status.CLIENT_ERROR_NOT_FOUND );
            }
            if (workspace == null && catalog.getStyleByName( xmark ) == null) {
                throw new RestletException( "No such xmark: " + xmark, Status.CLIENT_ERROR_NOT_FOUND );
            }
        }

        if ( xmark == null && request.getMethod() == Method.GET ) {
            return new XMarkListResource(getContext(),request,response,catalog);
        }

        return new XMarkResource(getContext(),request,response,catalog);
    }
}
