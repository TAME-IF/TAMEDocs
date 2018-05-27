var $Q1 = $Q1 || function(x)
{
	return document.querySelector(x);
}

var BodyElement = $Q1("body");
var DocsSidebar = $Q1("#tamedocs-sidebar");
var DocsSearchbar = $Q1("#tamedocs-search-input");

function tamedocsAddClass(elem, name)
{
	let classes = elem.className.trim().length > 0 ? elem.className.split("\\s+") : [];
	if (classes.indexOf(name) < 0)
	{
		classes.push(name);
		elem.className = classes.join(" ");
	}
}

function tamedocsRemoveClass(elem, name)
{
	let classes = elem.className.trim().length > 0 ? elem.className.split("\\s+") : [];
	let index = -1;
	if ((index = classes.indexOf(name)) >= 0)
	{
		classes.splice(index, 1);
		elem.className = classes.join(" ");
	}
}

function tamedocsOpenBar()
{
	DocsSidebar.style.display = "block";
}

function tamedocsCloseBar() 
{
	DocsSidebar.style.display = "none";
}

// Courtesy of https://stackoverflow.com/users/7173/jason
function tamedocsSelect(elementId)
{
	const element = document.getElementById(elementId);

    if (document.body.createTextRange) 
    {
        const range = document.body.createTextRange();
        range.moveToElementText(element);
        range.select();
    } 
    else if (window.getSelection) 
    {
        const selection = window.getSelection();
        const range = document.createRange();
        range.selectNodeContents(element);
        selection.removeAllRanges();
        selection.addRange(range);
    } 
    else 
    {
        console.warn("Could not select text in node: Unsupported browser.")
    }
}

function tamedocsClipboard(elementId)
{
	var elem = $Q1(elementId);
	tamedocsSelect(elementId);
	if (document.execCommand)
		document.execCommand('copy');
}

function tamedocsSearchUpdate()
{
	var input = DocsSearchbar.value;
	console.log(tamedocsSearch(input));
}

function tamedocsOpenSearchBar()
{
	DocsSearchbar.style.display = "inline";
}

function tamedocsCloseSearchBar() 
{
	DocsSearchbar.style.display = "none";
}

function tamedocsSearch(input)
{
	var tokenList = [];
	var searchTokens = input.trim().split(/\s+/);
	for (let x in searchTokens)
	{
		let p = searchTokens[x];
		let t = TAMEDOCS_SEARCH_INDEX['partials'][p];
		if (t) for (let i in t)
			tokenList.push(t[i]);
		else if (TAMEDOCS_SEARCH_INDEX['tokenDensity'][p])
			tokenList.push(p);
	}
	
	var output = null;
	for (let i in tokenList)
	{
		let stuff = [];
		let token = tokenList[i];
		let pageList = TAMEDOCS_SEARCH_INDEX['tokenDensity'][token];
		
		for (let page in pageList)
		{
			let score = pageList[page];
			stuff.push({"token":token, "title":TAMEDOCS_SEARCH_INDEX['pages'][page], "page":page, "relevance": score});
		}
		
		output = (output) ? output.concat(stuff) : stuff;
	}
	
	if (!output)
		output = [];
	
	// sort by closest relevance.
	output.sort(function(a,b){
		if (a.token === b.token)
			return b.relevance - a.relevance; 
		else
			return a.token.length - b.token.length;
	});

	// truncate to 20 results.
	output.length = Math.min(output.length, 20);

	return output;
}

// For sidebar highlight.
var LASTSIDEBAR_ELEM = null;
function tamedocsSidebarUpdate (location)
{
	if (location)
		location = location.substring(location.lastIndexOf('/') + 1, location.length);
	var elem = DocsSidebar.querySelector('a[href="'+location+'"]');
	if (LASTSIDEBAR_ELEM)
		tamedocsRemoveClass(LASTSIDEBAR_ELEM, 'tamedocs-sidebar-hilite');
	if (elem)
	{
		tamedocsAddClass(elem, 'tamedocs-sidebar-hilite');
		if (elem.scrollIntoView)
			elem.scrollIntoView();
	}
	LASTSIDEBAR_ELEM = elem;
};

var tamedocsSidebarListener;
(tamedocsSidebarListener = function(){tamedocsSidebarUpdate(window.location.href || document.URL);})();
window.addEventListener('popstate', tamedocsSidebarListener);
