/**
 * TAMEScript patterns for Rainbow
 * @author Matt Tropiano
 */
Rainbow.extend('tamescript', [
    {
        name: 'meta.preprocessor',
        matches: {
            1: [
                {
                    matches: {
                        1: 'keyword.define',
                        2: 'entity.name'
                    },
                    pattern: /(\w+)\s(\w+)\b/g
                },
                {
                    name: 'constant.numeric',
                    pattern: /\d+/g
                },
                {
                    matches: {
                        1: 'keyword.include',
                        2: 'string'
                    },
                    pattern: /(include)\s(.*?)$/g
                }
            ]
        },
        pattern: /\#([\S\s]*?)$/gm
    },
	{
		name: 'comment.multiline',
		pattern: /(^|[^\\])\/\*[\w\W]*?\*\//mg
	},
	{
		name: 'comment.singleline',
		pattern: /(^|[^\\:])\/\/.*/g
	},
    {
        name: 'keyword',
        pattern: /\b(if|else|while|for|quit|end|finish|break|continue|module|world|room|player|object|container|action|general|modal|transitive|ditransitive|open|restricted|named|tagged|modes|uses|conjunctions|determiners|forbids|allows|local|clear|archetype|function|return|this|override|extend|strict|reversed|queue)\b/gi
    },
    {
        name: 'command',
        pattern: /\b(NOOP|ADDCUE|TEXT|TEXT(F(LN)?|LN)|PAUSE|WAIT|AS(BOOLEAN|INT|FLOAT|STRING|LIST)|STR(LENGTH|CONCAT|REPLACE(LAST|ALL)?|INDEX|LASTINDEX|CONTAINS|STARTSWITH|ENDSWITH|LOWER|UPPER|CHAR|TRIM|FORMAT|JOIN|SPLIT)|SUBSTRING|ISREGEX|REGEX(CONTAINS|FIND(LAST)?|GET(LAST|ALL)?|MATCHES|SPLIT)|FLOOR|CEILING|ROUND|FIX|SQRT|PI|E|SIN|COS|TAN|MIN|MAX|CLAMP|IRANDOM|FRANDOM|GRANDOM|TIME|SECONDS|MINUTES|HOURS|DAYS|FORMATTIME|OBJECT(HAS(NAME|TAG|NOOWNER)?|COUNT)|ADDOBJECT(NAME|TAG(TOALLIN)?)|REMOVEOBJECT(NAME|TAG(FROMALLIN)?)?|GIVEOBJECT|MOVEOBJECTSWITHTAG|HASOBJECT|PLAYER(ISINROOM|CANACCESSOBJECT)|BROWSE(TAGGED)?|SET(PLAYER|ROOM)|PUSHROOM|POPROOM|SWAPROOM|CURRENT(PLAYERIS|ROOMIS)|NOCURRENT(PLAYER|ROOM)|IDENTITY|HEADER)\b/gi
    },
    {
        name: 'entry',
        pattern: /\b(INIT|AFTER(SUCCESSFUL|FAILED|EVERY)COMMAND|START|ON(ACTION(WITH(OTHER|ANCESTOR)?)?|MODALACTION|ELEMENTBROWSE|WORLDBROWSE|ROOMBROWSE|PLAYERBROWSE|CONTAINERBROWSE|AMBIGUOUSCOMMAND|MALFORMEDCOMMAND|INCOMPLETECOMMAND|UNKNOWNCOMMAND|UNHANDLEDACTION|FORBIDDENACTION|ROOMFORBIDDENACTION))\b/gi
    },
    {
        name: 'constant.keyword',
        pattern: /\b(true|false|Infinity|NaN)\b/gi
    },
	{
		name: 'string',
		pattern: /(["])(\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/g
	},
    {
        name: 'constant.numeric',
        pattern: /\b-?(?:0x[\da-f]+|\d*\.?\d+(?:e[+-]?\d+)?)\b/gi
    },
    {
        name: 'operator',
        pattern: /(\(|\)|\[|\]|:|\.|,|\+|\-|\!|~|\*\*?|\/|%|&\||\^|<<?|<=?|>>?>?|>=?|==?=?|\!==?)/g
    },
]);
