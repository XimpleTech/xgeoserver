<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a named layer is the basic building block of an sld document -->

  <NamedLayer>
    <Name>xtpc-dottedLine</Name>
    <UserStyle>
        <!-- they have names, titles and abstracts -->
      
      <Title>A boring default pgTPC-dotted_line style</Title>
      <Abstract>A sample style that just prints out a green line</Abstract>
      <!-- FeatureTypeStyles describe how to render different features -->
      <!-- a feature type for lines -->

      <FeatureTypeStyle>
        <!--FeatureTypeName>Feature</FeatureTypeName-->
        <Rule>
          <Name>pgTPCRule-dotted_line</Name>
          <Title>Rule Master for dotted line</Title>
          <Abstract>A green line with a dotted line</Abstract>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>symstyle</ogc:PropertyName><ogc:Literal>3</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
   <MaxScaleDenominator>5000</MaxScaleDenominator>
          <!-- like a polygonsymbolizer -->
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke"><ogc:PropertyName>symcolor</ogc:PropertyName></CssParameter>
              <CssParameter name="stroke-dasharray">5 5</CssParameter>
              <CssParameter name="stroke-dashoffset">5</CssParameter>
              <CssParameter name="stroke-width"><ogc:PropertyName>symweight</ogc:PropertyName></CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>