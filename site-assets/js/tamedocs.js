var $Q1 = $Q1 || function(x)
{
	return document.querySelector(x);
}

var $Q = $Q || function(x)
{
	return document.querySelectorAll(x);
}

// text: text in node
var $DOMText = function(text)
{
	return document.createTextNode(text);
};

// name: tagname
// attribs: object {attrname: 'value'}
// children: array of elements/nodes to append in order
var $DOMNew = function(name, attribs, children)
{
	let out = document.createElement(name);
	if (attribs) for (let a in attribs) if (attribs.hasOwnProperty(a))
	{
		let attrObj = document.createAttribute(a);
		attrObj.value = attribs[a];
		out.setAttributeNode(attrObj);
	}
	if (children) for (let i = 0; i < children.length; i++)
		out.appendChild(children[i]);
	
	return out;
};

// clears the descendants of an element.
// returns the element passed in
var $DOMClear = function(element) 
{
	while (element.firstChild)
		element.removeChild(element.firstChild);
	return element;
};

// clears the descendants of an element and appends new children.
// returns the element passed in
var $DOMReFill = function(element, newchildren)
{
	$DOMClear(element);
	for (let i = 0; i < newchildren.length; i++)
		element.appendChild(newchildren[i]);
	return element;
}

/** Important Elements ***************************************************/

var BodyElement = $Q1("body");
var DocsSidebar = $Q1("#tamedocs-sidebar");
var DocsSearchbar = $Q1("#tamedocs-search-input");
var DocsSearchResultBox = $Q1("#tame-search-results");
var DocsSearchResults = $Q1("#tame-search-result-list");

/*************************************************************************/

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
	tamedocsSelect(elementId);
	if (document.execCommand)
		document.execCommand('copy');
}

function tamedocsSearchUpdate()
{
	var input = DocsSearchbar.value;
	var result = tamedocsSearch(input, 20);
	tamedocsSearchResultUpdate(result);
	DocsSearchResultBox.style.display = result.length > 0 ? "inline" : "none";
}

function tamedocsSearchResultUpdate(result)
{
	var elems = [];
	
	for (let r = 0; r < result.length; r++)
	{
		elems.push(
			$DOMNew('li', null, [
				$DOMNew('a', {"href":result[r].page}, [
					$DOMText(result[r].token),
					$DOMNew('span', null, [
						$DOMText(result[r].title + ": " + result[r].page)
					])
				])
			])
		);
	}
	
	$DOMReFill(DocsSearchResults, elems);
}

function tamedocsToggleSearchBar()
{
	if (DocsSearchbar.style.display === "inline")
		tamedocsCloseSearchBar();
	else if (DocsSearchbar.style.display === "none")
		tamedocsOpenSearchBar();
}

function tamedocsOpenSearchBar()
{
	DocsSearchbar.style.display = "inline";
}

function tamedocsCloseSearchBar() 
{
	DocsSearchbar.style.display = "none";
	DocsSearchResultBox.style.display = "none";
}

function tamedocsSearch(input, limit)
{
	if (DocsSearchbar.style.display !== "inline")
		return [];
	
	var tokenList = [];
	var searchTokens = input.trim().split(/\s+/);
	for (let x in searchTokens)
	{
		let p = searchTokens[x];
		let t = TAMEDOCS_SEARCH_INDEX['partials'][p];
		if (TAMEDOCS_SEARCH_INDEX['tokenDensity'][p])
			tokenList.push(p);
		else if (t) for (let i in t)
			tokenList.push(t[i]);
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

	// truncate to [limit] results.
	output.length = Math.min(output.length, limit);

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
