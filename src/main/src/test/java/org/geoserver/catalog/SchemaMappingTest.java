/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class SchemaMappingTest extends GeoServerSystemTestSupport {

    public SchemaMappingTest() {
        super();
    }
    
    @Before
    public void removeMappings() throws IOException {
        File resourceDir = getDataDirectory().findResourceDir(getDividedRoutes());
        new File(resourceDir, "schema.xsd").delete();
        new File(resourceDir, "schema.xml").delete();
    }

    @Test
    public void testNoMapping() throws Exception {
        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 4, ft.attributes().size() );
    }

    @Test
    public void testXsdMapping() throws Exception {
        getDataDirectory().copyToResourceDir(
            getDividedRoutes(), getClass().getResourceAsStream( "schema.xsd"), "schema.xsd");

        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 3, ft.attributes().size() );
    }
    
    @Test
    public void testXmlMapping() throws Exception {
        getDataDirectory().copyToResourceDir(
                getDividedRoutes(), getClass().getResourceAsStream( "schema.xml"), "schema.xml");

        reloadCatalogAndConfiguration();
        FeatureTypeInfo ft = 
            getCatalog().getFeatureTypeByName( "DividedRoutes");
        assertEquals( 2, ft.attributes().size() );
    }

    FeatureTypeInfo getDividedRoutes() {
        return getCatalog().getFeatureTypeByName( "DividedRoutes");
    }
}