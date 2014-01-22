<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld"
                           xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml"
                           version="1.0.0">
  <sld:UserLayer>
    <sld:LayerFeatureConstraints>
      <sld:FeatureTypeConstraint/>
    </sld:LayerFeatureConstraints>
    <sld:UserStyle>
      <sld:Name>Default Styler</sld:Name>
      <sld:Title/>
      <sld:FeatureTypeStyle>
        <sld:Name>Group0</sld:Name>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
        <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
        <sld:Rule>
          <sld:Name>Default Rule Symbol</sld:Name>
          <sld:MaxScaleDenominator>100</sld:MaxScaleDenominator>
          <sld:PolygonSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName>geom</ogc:PropertyName>
            </sld:Geometry>
            <sld:Fill>
              <sld:CssParameter name="fill">#D95F02</sld:CssParameter>
              <sld:CssParameter name="fill-opacity">0.1</sld:CssParameter>
            </sld:Fill>
            <sld:Stroke>
              <sld:CssParameter name="stroke">#D95F02</sld:CssParameter>
              <sld:CssParameter name="stroke-opacity">0.25</sld:CssParameter>
            </sld:Stroke>
          </sld:PolygonSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:Name>pgTPC Rule Symbol</sld:Name>
          <sld:Title>pgSymbolMark</sld:Title>
          <sld:MaxScaleDenominator>5600</sld:MaxScaleDenominator>
          <!--sld:PointSymbolizer uom="http://www.opengeospatial.org/se/units/metre"-->
          <sld:PointSymbolizer>
            <sld:Geometry>
              <ogc:PropertyName>origin</ogc:PropertyName>
            </sld:Geometry>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>
                  <ogc:Function name="concatenate">
                    <ogc:Literal>xshape://</ogc:Literal>
                    <ogc:PropertyName>symbol</ogc:PropertyName>
                  </ogc:Function>
                </sld:WellKnownName>
                <sld:Stroke>
                  <sld:CssParameter name="stroke">
                    <ogc:PropertyName>symcolor</ogc:PropertyName>
                  </sld:CssParameter>
                  <sld:CssParameter name="stroke-width">0.5</sld:CssParameter>
                  <!--sld:CssParameter name="stroke-opacity">0.5</sld:CssParameter-->
                </sld:Stroke>
              </sld:Mark>
              <sld:Size>
                <ogc:PropertyName>height</ogc:PropertyName>
              </sld:Size>
              <sld:Rotation>
                <ogc:PropertyName>angle</ogc:PropertyName>
              </sld:Rotation>
              <sld:Width>
                <ogc:PropertyName>width</ogc:PropertyName>
              </sld:Width>
            </sld:Graphic>
            <sld:VendorOption name="ZOOMBYSCALE">true</sld:VendorOption>
          </sld:PointSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:Name>pgTPC Rule LocatePoint</sld:Name>
          <sld:Title>RedSquare</sld:Title>
          <sld:MaxScaleDenominator>50</sld:MaxScaleDenominator>
          <sld:PointSymbolizer uom="http://www.opengeospatial.org/se/units/metre">
            <sld:Geometry>
              <ogc:PropertyName>origin</ogc:PropertyName>
            </sld:Geometry>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>square</sld:WellKnownName>
                <sld:Fill>
                  <sld:CssParameter name="fill">#FF0000</sld:CssParameter>
                  <sld:CssParameter name="stroke-width">0.25</sld:CssParameter>
                  <sld:CssParameter name="stroke-opacity">0.1</sld:CssParameter>
                </sld:Fill>
              </sld:Mark>
              <sld:Size>0.1</sld:Size>
            </sld:Graphic>
          </sld:PointSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>