/* Fades the background video in on the landing and sign-in pages, and plays it
   slowly and smoothly.

   RATE: 1 is normal speed. 0.6 makes the clip about 1.7 times longer and calmer.
   Lower it (for example 0.5) for slower, raise it (0.8) for faster. */
(function () {
    var RATE = 0.6;
    var v = document.querySelector('.cine-video');
    if (!v) { return; }

    function slow() {
        v.defaultPlaybackRate = RATE;
        v.playbackRate = RATE;
    }

    var shown = false;
    function show() {
        if (shown) { return; }
        shown = true;
        slow();
        v.classList.add('on');
        var p = v.play ? v.play() : null;
        if (p && p.catch) { p.catch(function () {}); }
    }

    slow();
    if (v.readyState >= 3) { show(); }
    ['loadedmetadata', 'loadeddata', 'canplay', 'playing'].forEach(function (ev) {
        v.addEventListener(ev, function () { slow(); show(); });
    });
    /* Some browsers reset the speed when the loop restarts, so set it again. */
    v.addEventListener('ratechange', function () { if (v.playbackRate !== RATE) { slow(); } });
    window.addEventListener('load', function () { if (v.readyState >= 2) { show(); } });
    setTimeout(function () { if (v.readyState >= 2) { show(); } }, 1500);
})();
