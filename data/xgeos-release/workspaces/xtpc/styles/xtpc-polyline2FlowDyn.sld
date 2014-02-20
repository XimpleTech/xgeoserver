<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
        <sld:UserStyle>
            <sld:Name>fsc-106-c-0</sld:Name>
            <sld:Title/>
            <sld:FeatureTypeStyle>
                <sld:Name>name</sld:Name>
                <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
                <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
                <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
                <sld:Rule>
                    <sld:Name>pgTPCRule-Master2</sld:Name>
                    <sld:Title>Rule Master for WT2</sld:Title>
                    <ogc:Filter>
                        <ogc:PropertyIsEqualTo>
                            <ogc:PropertyName>symweight</ogc:PropertyName>
                            <ogc:Literal>2</ogc:Literal>
                        </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">
                                <ogc:PropertyName>dyncolor</ogc:PropertyName>
                            </sld:CssParameter>
                            <sld:CssParameter name="stroke-width">
                                <ogc:PropertyName>symweight</ogc:PropertyName>
                            </sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:GraphicStroke>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName><ogc:PropertyName>flow</ogc:PropertyName></sld:WellKnownName>
                                        <sld:Fill>
                                            <sld:CssParameter name="fill">
                                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                                            </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">0.2</sld:CssParameter>
                                        </sld:Fill>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">
                                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                                            </sld:CssParameter>
                                            <sld:CssParameter name="stroke-opacity">0.2</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>8</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicStroke>
                            <sld:CssParameter name="stroke">
                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                            </sld:CssParameter>
                            <sld:CssParameter name="stroke-opacity">0.2</sld:CssParameter>
                            <!--sld:CssParameter name="stroke-dashoffset">80</sld:CssParameter-->
                            <sld:CssParameter name="stroke-dasharray">8 48</sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                </sld:Rule>
                <sld:Rule>
                    <sld:Name>pgTPCRule-Master1</sld:Name>
                    <sld:Title>Rule Master for pgTPC-Conductor WT1</sld:Title>
                    <ogc:Filter>
                        <ogc:PropertyIsEqualTo>
                            <ogc:PropertyName>symweight</ogc:PropertyName>
                            <ogc:Literal>1</ogc:Literal>
                        </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                    <sld:MaxScaleDenominator>30000.0</sld:MaxScaleDenominator>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">
                                <ogc:PropertyName>dyncolor</ogc:PropertyName>
                            </sld:CssParameter>
                            <sld:CssParameter name="stroke-width">
                                <ogc:PropertyName>symweight</ogc:PropertyName>
                            </sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:GraphicStroke>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName><ogc:PropertyName>flow</ogc:PropertyName></sld:WellKnownName>
                                        <sld:Fill>
                                            <sld:CssParameter name="fill">
                                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                                            </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">0.2</sld:CssParameter>
                                        </sld:Fill>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">
                                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                                            </sld:CssParameter>
                                            <sld:CssParameter name="stroke-opacity">0.2</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>8</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicStroke>
                            <sld:CssParameter name="stroke">
                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                            </sld:CssParameter>
                            <sld:CssParameter name="stroke-opacity">0.2</sld:CssParameter>
                            <!--sld:CssParameter name="stroke-dashoffset">80</sld:CssParameter-->
                            <sld:CssParameter name="stroke-dasharray">8 48</sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                </sld:Rule>
                <sld:Rule>
                    <sld:Name>pgTPCRule-Master0</sld:Name>
                    <sld:Title>Rule Master for pgTPC-Conductor WT0</sld:Title>
                    <ogc:Filter>
                        <ogc:PropertyIsEqualTo>
                            <ogc:PropertyName>symweight</ogc:PropertyName>
                            <ogc:Literal>0</ogc:Literal>
                        </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                    <sld:MaxScaleDenominator>5000.0</sld:MaxScaleDenominator>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">
                                <ogc:PropertyName>dyncolor</ogc:PropertyName>
                            </sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:GraphicStroke>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName><ogc:PropertyName>flow</ogc:PropertyName></sld:WellKnownName>
                                        <sld:Fill>
                                            <sld:CssParameter name="fill">
                                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                                            </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">0.2</sld:CssParameter>
                                        </sld:Fill>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">
                                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                                            </sld:CssParameter>
                                            <sld:CssParameter name="stroke-opacity">0.2</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>8</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicStroke>
                            <sld:CssParameter name="stroke">
                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                            </sld:CssParameter>
                            <sld:CssParameter name="stroke-opacity">0.2</sld:CssParameter>
                            <!--sld:CssParameter name="stroke-dashoffset">80</sld:CssParameter-->
                            <sld:CssParameter name="stroke-dasharray">8 48</sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:UserLayer>
</sld:StyledLayerDescriptor>