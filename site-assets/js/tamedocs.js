var $Q1 = $Q1 || function(x)
{
	return document.querySelector(x);
}

var BodyElement = $Q1("body");
var DocsSidebar = $Q1("#tamedocs-sidebar");

function tamedocsOpenBar()
{
	DocsSidebar.style.display = "block";
}

function tamedocsCloseBar() 
{
	DocsSidebar.style.display = "none";
}
