<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer><Name>Simple Roads</Name>
  <UserStyle>
    
    <Title>Default Styler for simple road segments</Title>
    <Abstract></Abstract>
    <FeatureTypeStyle>
      <FeatureTypeName>Feature</FeatureTypeName>
      <Rule>
        <Name>simple roads</Name>
        <Title>Simple road segments</Title>
        <MaxScaleDenominator>5000</MaxScaleDenominator>
        <LineSymbolizer>
          <Stroke>
             <CssParameter name="stroke"><ogc:Literal>#1B9E77</ogc:Literal></CssParameter>
             <CssParameter name="stroke-linecap"><ogc:Literal>butt</ogc:Literal></CssParameter>
             <CssParameter name="stroke-linejoin"><ogc:Literal>miter</ogc:Literal></CssParameter>
             <CssParameter name="stroke-opacity"><ogc:Literal>0.85</ogc:Literal></CssParameter>
             <CssParameter name="stroke-width"><ogc:Literal>1.0</ogc:Literal></CssParameter>
             <CssParameter name="stroke-dashoffset"><ogc:Literal>0.0</ogc:Literal></CssParameter>
          </Stroke>
        </LineSymbolizer>
      </Rule>
      <Rule>
        <Name>simple roads 1</Name>
        <Title>Simple road segments</Title>
        <ogc:Filter>
          <ogc:PropertyIsEqualTo>
            <ogc:PropertyName>symweight</ogc:PropertyName>
            <ogc:Literal>0</ogc:Literal>
          </ogc:PropertyIsEqualTo>
        </ogc:Filter>
        <MaxScaleDenominator>5000</MaxScaleDenominator>
        <LineSymbolizer>
          <Stroke>
             <CssParameter name="stroke"><ogc:Literal>#1B9E77</ogc:Literal></CssParameter>
             <CssParameter name="stroke-linecap"><ogc:Literal>butt</ogc:Literal></CssParameter>
             <CssParameter name="stroke-linejoin"><ogc:Literal>miter</ogc:Literal></CssParameter>
             <CssParameter name="stroke-opacity"><ogc:Literal>0.85</ogc:Literal></CssParameter>
             <CssParameter name="stroke-width"><ogc:Literal>1.0</ogc:Literal></CssParameter>
             <CssParameter name="stroke-dashoffset"><ogc:Literal>0.0</ogc:Literal></CssParameter>
          </Stroke>
        </LineSymbolizer>
      </Rule>
    </FeatureTypeStyle>
  </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>