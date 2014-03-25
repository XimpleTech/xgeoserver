<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a named layer is the basic building block of an sld document -->

    <NamedLayer>
      <Name>pgTPC_Text2</Name>
      <UserStyle>
      <!-- they have names, titles and abstracts -->
      <Title>A boring default pgText2 style</Title>
      <Abstract>A sample style that just prints out a transparent red interior with a red outline</Abstract>
      <!-- FeatureTypeStyles describe how to render different features -->
      <!-- a feature type for polygons -->

      <FeatureTypeStyle>
        <!--FeatureTypeName>Feature</FeatureTypeName-->
        <Rule>
          <Name>pgTextLabel_2</Name>
          <Title>Textlabel RedFill RedOutline</Title>
          <Abstract>50% transparent red fill with a red outline 1 pixel in width</Abstract>
          <!-- like a linesymbolizer but with a fill too -->
          <MaxScaleDenominator>200</MaxScaleDenominator>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>context</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family"><ogc:Literal>Dialog</ogc:Literal></CssParameter>
              <CssParameter name="font-size"><ogc:Literal>12.0</ogc:Literal></CssParameter>
              <CssParameter name="font-style"><ogc:Literal>normal</ogc:Literal></CssParameter>
              <CssParameter name="font-weight"><ogc:Literal>normal</ogc:Literal></CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX><ogc:Literal>0.6</ogc:Literal></AnchorPointX>
                  <AnchorPointY><ogc:Literal>0.0</ogc:Literal></AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX><ogc:Literal>0.0</ogc:Literal></DisplacementX>
                  <DisplacementY><ogc:Literal>0.0</ogc:Literal></DisplacementY>
                </Displacement>
                <Rotation><ogc:PropertyName>angle</ogc:PropertyName></Rotation>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill"><ogc:PropertyName>symcolor</ogc:PropertyName></CssParameter>
              <CssParameter name="fill-opacity"><ogc:Literal>1.0</ogc:Literal></CssParameter>
            </Fill>
            <VendorOption name="spaceAround">2</VendorOption>
          </TextSymbolizer>
        </Rule>
        <Rule>
          <Name>pgTPC Rule LocatePoint2</Name>
          <Title>pgTpcLabelRedSquare2</Title>
          <Abstract>A red fill with an 11 pixel size</Abstract>

          <!-- like a linesymbolizer but with a fill too -->
          <MaxScaleDenominator>200</MaxScaleDenominator>
          <PointSymbolizer>
             <Graphic>
                <Mark>
                   <WellKnownName>square</WellKnownName>
                   <Fill>
                      <CssParameter name="fill">#008000</CssParameter>
                   </Fill>
                </Mark>
                <Size>3</Size>
             </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>