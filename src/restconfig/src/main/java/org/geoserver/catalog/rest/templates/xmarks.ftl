<#include "head.ftl">
XMarks:
<ul>
<#list values as m>
  <li><a href="${page.pageURI(m.properties.name + '.html')}">${m.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
