(function () {
var hideElements = function () {
    // no need to invoke if already invoked on another event
    if (document.{{HIDDEN_FLAG}} === true) {
        {{DEBUG}} console.log('already hidden, exiting');
        return;
    }

    {{DEBUG}} console.log('not yet hidden!')

    // hide by injecting CSS stylesheet
    {{DEBUG}} console.log('starting injecting eh css rules for ' + document.location.href);
    var styleSheet = {{BRIDGE}}.getEleHidingStyleSheet(document.location.href);
    {{DEBUG}} console.log('stylesheet length: ' + styleSheet.length);
    if (styleSheet) {
        var head = document.getElementsByTagName('head')[0];
        var style = document.createElement('style');
        head.appendChild(style);
        style.textContent = styleSheet;
        {{DEBUG}} console.log('finished injecting css rules');
    } else {
        {{DEBUG}} console.log('stylesheet is empty, skipping injection');
    }

    document.{{HIDDEN_FLAG}} = true; // set flag not to do it again
};

if (document.readyState === 'complete') {
    {{DEBUG}} console.log('document is in "complete" state, apply hiding')
    hideElements();
} else {
    {{DEBUG}} console.log('installing listener')

    document.onreadystatechange = function () {
        {{DEBUG}} console.log('onreadystatechange() event fired (' + document.readyState + ')')
        if (document.readyState == 'interactive') {
            hideElements();
        }
    }

    window.addEventListener('load', function (event) {
        {{DEBUG}} console.log('load() event fired');
        hideElements();
    });

    document.addEventListener('DOMContentLoaded', function () {
        {{DEBUG}} console.log('DOMContentLoaded() event fired');
        hideElements();
    }, false);
}
})();