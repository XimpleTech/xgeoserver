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
                <sld:Name>群組0</sld:Name>
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
                                <ogc:PropertyName>symcolor</ogc:PropertyName>
                            </sld:CssParameter>
                            <sld:CssParameter name="stroke-width">
								<ogc:PropertyName>symweight</ogc:PropertyName>
							</sld:CssParameter>
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
					<sld:MaxScaleDenominator>30000</sld:MaxScaleDenominator>
	          		<sld:LineSymbolizer>
	            		<sld:Stroke>
	              			<sld:CssParameter name="stroke"><ogc:PropertyName>symcolor</ogc:PropertyName></sld:CssParameter>
	              			<sld:CssParameter name="stroke-width"><ogc:PropertyName>symweight</ogc:PropertyName></sld:CssParameter>
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
					<sld:MaxScaleDenominator>5000</sld:MaxScaleDenominator>
	          		<sld:LineSymbolizer>
	            		<sld:Stroke>
	              			<sld:CssParameter name="stroke"><ogc:PropertyName>symcolor</ogc:PropertyName></sld:CssParameter>
	              			<sld:CssParameter name="stroke-width"><ogc:Literal>1</ogc:Literal></sld:CssParameter>
	            		</sld:Stroke>
					</sld:LineSymbolizer>
	        	</sld:Rule>                
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:UserLayer>
</sld:StyledLayerDescriptor>

