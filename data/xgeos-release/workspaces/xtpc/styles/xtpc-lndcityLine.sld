<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a named layer is the basic building block of an sld document -->

    <NamedLayer>
        <Name>xTpc LndCity Line</Name>
        <UserStyle>
            <!-- they have names, titles and abstracts -->

            <Title>A boring default LndCity Line style</Title>
            <Abstract>A sample style that just prints out a LndCity line</Abstract>
            <!-- FeatureTypeStyles describe how to render different features -->
            <!-- a feature type for lines -->

            <FeatureTypeStyle>
                <!--FeatureTypeName>Feature</FeatureTypeName-->
                <Rule>
                    <Name>Rule Line</Name>
                    <Title>Gray Line</Title>
                    <Abstract>A gray line with a 1 pixel width</Abstract>
                    <!-- like a polygonsymbolizer -->
                    <MaxScaleDenominator>10000</MaxScaleDenominator>
                    <LineSymbolizer>
                        <Stroke>
                          <CssParameter name="stroke">#1B9E77</CssParameter>
                          <CssParameter name="stroke-opacity">0.85</CssParameter>
                          <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                    </LineSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>