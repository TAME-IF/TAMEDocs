var $Q1 = $Q1 || function(x)
{
	return document.querySelector(x);
}

var ModalBox = $Q1("#tame-modal");
var ModalTitle = $Q1("#tame-title");
var InputBox = $Q1("#tame-input");
var OutputBox = $Q1("#tame-output");
var InputDiv = $Q1("#tame-modal-input");
var OutputDiv = $Q1("#tame-modal-output");
var ContinueDiv = $Q1("#tame-modal-continue");

function tameReset(heading)
{
	ModalTitle.innerHTML = heading;
	OutputBox.innerHTML = "";
	
	InputDiv.style.display = "block";
	ContinueDiv.style.display = "none";

	InputBox.disabled = false;
	InputBox.value = "";
	InputBox.focus();
}

function tamePrint(text) 
{
	if (!text)
		return;
	OutputBox.appendChild($DOMNew('span',{},[$DOMText(text)]));
	OutputDiv.scrollTop = OutputDiv.scrollHeight;
}

function tamePrintln(text) 
{
	if (!text)
		tamePrint('\n');
	else
		tamePrint(text + '\n');
}

var TameStop = false;
var CurrentModuleContext = null;
var TameDebug = false;
var TameTrace = false;

function withEscChars(text) 
{
	var t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
}

function tameDebugHandleCue(cue) 
{
	tamePrintln('['+cue.type+'] '+withEscChars(cue.content));
	var type = cue.type.toLowerCase();
	if (type === 'quit' || type === 'fatal')
		TameStop = true;
	return true;
}

function tameDebugResponse(response)
{
	tamePrintln('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
	tamePrintln('Request time: '+(response.requestNanos/1000000.0)+' ms');
	tamePrintln('Commands: '+response.commandsExecuted);
	tamePrintln('Cues: '+response.responseCues.length);

	for (i in response.responseCues) if (response.responseCues.hasOwnProperty(i))
		tameDebugHandleCue(response.responseCues[i]);
	
	if (TameStop)
	{
		InputBox.disabled = true;
		InputDiv.style.display = "none";
		ContinueDiv.style.display = "none";
	}

	tamePrintln();
}


var TAMEHandler = TAME.newBrowserHandler({
	
	"print": tamePrint,
	
	"onStart": function()
	{
		InputBox.disabled = true;	
	},
	
	"onSuspend": function() 
	{
		InputBox.disabled = false;
	},
	
	"onResume": function()
	{
		InputBox.disabled = true;
	},
	
	"onEnd": function()
	{
		InputBox.disabled = false;
		InputBox.focus();
	},
	
	"onPauseCue": function()
	{
		InputBox.disabled = true;
		InputDiv.style.display = "none";
		ContinueDiv.style.display = "block";
	},
	
	"onQuitCue": function(content)
	{
		InputBox.disabled = true;
		InputDiv.style.display = "none";
		ContinueDiv.style.display = "none";
	},
	
	"onErrorCue": function(content)
	{
		tamePrintln("\n !ERROR!: "+content);
	},
	
	"onFatalCue": function(content)
	{
		tamePrintln("\n!!FATAL!!: "+content);
		InputBox.disabled = true;
		InputDiv.style.display = "none";
		ContinueDiv.style.display = "none";
	}

});

function tameStartExample(heading, module, debug, trace)
{
	tameReset(heading);
	ModalBox.style.display = "block";
	
	TameStop = false;
	TameDebug = debug;
	TameTrace = trace;
	CurrentModuleContext = TAME.newContext(TAME.createModule(module));
	
	if (TameDebug)
	{
		tameDebugResponse(TAME.initialize(CurrentModuleContext, TameTrace));
	}
	else
	{
		TAMEHandler.prepare(TAME.initialize(CurrentModuleContext, TameTrace));
		TAMEHandler.resume();
	}
	
}

function tameHideModal()
{
	ModalBox.style.display = "none";
	CurrentModuleContext = null;
	TameDebug = false;
	TameTrace = false;
}

function tameInput()
{
	if (!CurrentModuleContext)
		return;
	
	var val = InputBox.value;
	InputBox.value = '';
	tamePrintln("\n> "+val);
	
	if (TameDebug)
	{
		tameDebugResponse(TAME.interpret(CurrentModuleContext, val, TameTrace));
	}
	else
	{
		TAMEHandler.prepare(TAME.interpret(CurrentModuleContext, val, TameTrace));
		TAMEHandler.resume();
	}

}

function tameContinue()
{
	InputDiv.style.display = "block";
	ContinueDiv.style.display = "none";
	TAMEHandler.resume();
}

BodyElement.onload = function() 
{	
	InputBox.addEventListener("keydown", function(event) 
	{
		// enter
		if (event.keyCode == 13) 
		{
			event.preventDefault();
			tameInput();
		}
	});
	
	for (let i = 0; i < TAMEDOCS_MISSINGPAGES.length; i++)
	{
		let elems = $Q('a[href^="'+TAMEDOCS_MISSINGPAGES[i]+'"]');
		for (let x = 0; x < elems.length; x++)
			tamedocsAddClass(elems[x], 'missing');
	}
	
};
