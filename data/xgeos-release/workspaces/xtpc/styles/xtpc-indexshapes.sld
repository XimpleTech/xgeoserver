<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a named layer is the basic building block of an sld document -->

  <NamedLayer>
    <Name>xtpc-IndexShapes_Grid_Text</Name>
    <UserStyle>
      <!-- they have names, titles and abstracts -->
      
      <Title>A boring pgIndexshape default style</Title>
      <Abstract>A sample style that just prints out a transparent red interior with a red outline</Abstract>
      <!-- FeatureTypeStyles describe how to render different features -->
      <!-- a feature type for polygons -->
      <FeatureTypeStyle>
        <!--FeatureTypeName>Feature</FeatureTypeName-->
        <Rule>
          <Name>pgIndexShapes TEXT 1</Name>
          <Title>pgIndexShapes TEXT</Title>
          <Abstract>50% transparent red fill with a red outline 1 pixel in width</Abstract>
          <MaxScaleDenominator>10000</MaxScaleDenominator>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>tpclid</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family"><ogc:Literal>Arial</ogc:Literal></CssParameter>
              <CssParameter name="font-size"><ogc:Literal>12.0</ogc:Literal></CssParameter>
              <CssParameter name="font-style"><ogc:Literal>normal</ogc:Literal></CssParameter>
              <CssParameter name="font-weight"><ogc:Literal>normal</ogc:Literal></CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX><ogc:Literal>0.0</ogc:Literal></AnchorPointX>
                  <AnchorPointY><ogc:Literal>0.0</ogc:Literal></AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX><ogc:Literal>0.0</ogc:Literal></DisplacementX>
                  <DisplacementY><ogc:Literal>0.0</ogc:Literal></DisplacementY>
                </Displacement>
                <Rotation><ogc:Literal>0.0</ogc:Literal></Rotation>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill"><ogc:Literal>#000000</ogc:Literal></CssParameter>
              <CssParameter name="fill-opacity"><ogc:Literal>1.0</ogc:Literal></CssParameter>
            </Fill>
          </TextSymbolizer>

        </Rule>
        <Rule>
           <Name>pgIndexShapes Rule Grid 1</Name>
           <Title> pgIndexShapes   Rule Grid</Title>
           <Abstract>Abstract</Abstract>
           <MaxScaleDenominator>50000</MaxScaleDenominator>
           <LineSymbolizer>
              <Stroke>
                 <CssParameter name="stroke"><ogc:Literal>#8B0000</ogc:Literal></CssParameter>
                 <CssParameter name="stroke-linecap"><ogc:Literal>butt</ogc:Literal></CssParameter>
                 <CssParameter name="stroke-linejoin"><ogc:Literal>miter</ogc:Literal></CssParameter>
                 <CssParameter name="stroke-opacity"><ogc:Literal>1</ogc:Literal></CssParameter>
                 <CssParameter name="stroke-width"><ogc:Literal>1</ogc:Literal></CssParameter>
                 <CssParameter name="stroke-dashoffset"><ogc:Literal>0</ogc:Literal></CssParameter>
              </Stroke>
          </LineSymbolizer>
       </Rule>
     </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>