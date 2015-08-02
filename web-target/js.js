/*
   Copyright 2015 Christian Felde

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

google.load("visualization", "1.1", {packages:["corechart"]});
google.setOnLoadCallback(initTitleScroll);
google.setOnLoadCallback(initPieScrolls);

var chart1;
var chart2;

function drawChart(data) {
    if (typeof chart1 === 'undefined') {
        chart1 = new google.visualization.SteppedAreaChart(document.getElementById('chart'));
    };

    var options = {
        title: 'Historical employee distribution',
        isStacked: 'percent',
        height: 200,
        width: '80%',
        legend: {position: 'none', maxLines: 3},
        focusTarget: 'category',
        vAxis: {
            minValue: 0,
            textPosition: 'none'
        },
        hAxis: {
            textPosition: 'none'
        },
        series: [
            {color: 'black'},
            {color: 'red'},
            {color: 'yellow'},
            {color: 'green'},
            {color: 'blue'}
        ]
    };

    chart1.draw(google.visualization.arrayToDataTable(data), options);
}

function drawPie(data) {
    if (typeof chart2 === 'undefined') {
        chart2 = new google.visualization.PieChart(document.getElementById('piechart'));
    };

    var options = {
        title: 'Current employee distribution',
        pieHole: 0.4,
        pieSliceText: 'none',
        colors: ['blue', 'green', 'yellow', 'red', 'grey'],
        legend: {
            position: 'none'
        },
        tooltip: {
            text: 'percentage'
        }
    };

    chart2.draw(google.visualization.arrayToDataTable(data), options);
}

function notifyConfig(update) {
    blender_corp_web.core.config_update(update);
}

function initMenu(propsAll, props) {
    var gui = new dat.GUI( { load: propsAll, width: 400 } );

    gui.remember(props);

    var fHire = gui.addFolder('Hire');
    fHire.add(props, 'Hire A').onChange(function(v) { notifyConfig({ hire: { a: v } }) });
    fHire.add(props, 'Hire B').onChange(function(v) { notifyConfig({ hire: { b: v } }) });
    fHire.add(props, 'Hire C').onChange(function(v) { notifyConfig({ hire: { c: v } }) });
    fHire.add(props, 'Hire D').onChange(function(v) { notifyConfig({ hire: { d: v } }) });
    fHire.add(props, 'Hire F').onChange(function(v) { notifyConfig({ hire: { f: v } }) });

    var fFire = gui.addFolder('Fire');
    fFire.add(props, 'Fire A').onChange(function(v) { notifyConfig({ fire: { a: v } }) });
    fFire.add(props, 'Fire B').onChange(function(v) { notifyConfig({ fire: { b: v } }) });
    fFire.add(props, 'Fire C').onChange(function(v) { notifyConfig({ fire: { c: v } }) });
    fFire.add(props, 'Fire D').onChange(function(v) { notifyConfig({ fire: { d: v } }) });
    fFire.add(props, 'Fire F').onChange(function(v) { notifyConfig({ fire: { f: v } }) });
    fFire.add(props, 'Fire turn over (%)').min(0).max(100).step(1).onChange(function(v) { notifyConfig({ fireturnover: v }) });

    var fHighSkill = gui.addFolder('Highly skilled');
    fHighSkill.add(props, 'A is high skill?').onChange(function(v) { notifyConfig({ highskill: { a: v } }) });
    fHighSkill.add(props, 'B is high skill?').onChange(function(v) { notifyConfig({ highskill: { b: v } }) });
    fHighSkill.add(props, 'C is high skill?').onChange(function(v) { notifyConfig({ highskill: { c: v } }) });
    fHighSkill.add(props, 'D is high skill?').onChange(function(v) { notifyConfig({ highskill: { d: v } }) });
    fHighSkill.add(props, 'F is high skill?').onChange(function(v) { notifyConfig({ highskill: { f: v } }) });

    gui.add(props, 'Turn over (%)').min(0).max(100).step(1).onChange(function(v) { notifyConfig({ turnover: v }) });
    gui.add(props, 'Level up (%)').min(0).max(100).step(1).onChange(function(v) { notifyConfig({ levelup: v }) });
    gui.add(props, 'Level down (%)').min(0).max(100).step(1).onChange(function(v) { notifyConfig({ leveldown: v }) });
    gui.add(props, 'Group dynamics boost (%)').min(0).max(100).step(1).onChange(function(v) { notifyConfig({ gdb: v }) });

    gui.add(props, 'Pause?').onChange(function(v) { notifyConfig({ pause: v }) });

    gui.close();
}

function findPosY(obj) {
    var curtop = 0;
    if (typeof (obj.offsetParent) != 'undefined' && obj.offsetParent) {
        while (obj.offsetParent) {
            curtop += obj.offsetTop;
            obj = obj.offsetParent;
        }
        curtop += obj.offsetTop;
    } else if (obj.y) {
        curtop += obj.y;
    }

    return curtop;
}

function initTitleScroll() {
    var startPos = -1;
    var title = document.getElementById('title');
    var titleSpace = document.getElementById('title-space');
    var isRelative = true;

    var fn = function() {
        if (isRelative) {
            startPos = findPosY(title);
        }

        if (pageYOffset < startPos) {
            title.className = 'title-relative';
            titleSpace.style.display = 'none';
            isRelative = true;
        } else {
            title.className = 'title-fixed';
            titleSpace.style.display = 'block';
            isRelative = false;
        }
    };

    window.addEventListener("scroll", fn);
    window.addEventListener("resize", fn);
    window.addEventListener("orientationChange", fn);

    setTimeout(function() { fn(); }, 1000);
}

function initPieScroll(obj) {
    var imgPos = -1;
    var sectionPos = -1;
    var pparent = obj.parentElement.parentElement;
    var imgHeight = obj.clientHeight;

    var fn = function() {
        if (imgPos < 0 || sectionPos < 0) {
            imgPos = findPosY(obj);
            sectionPos = findPosY(pparent);
        }

        var sectionHeight = pparent.clientHeight;
        var viewportHeight = document.documentElement.clientHeight;

        var targetPos = Math.floor(pageYOffset + (viewportHeight / 2) - (imgHeight / 2));

        if (targetPos < sectionPos) {
            obj.style.top = '0px';
        } else if (targetPos > sectionPos + sectionHeight - imgHeight) {
            obj.style.top = (sectionHeight - imgHeight) + 'px';
        } else {
            obj.style.top = (targetPos - sectionPos) + 'px';
        }
    };

    var fn2 = function() {
        sectionPos = -1;
        fn();
    }

    window.addEventListener("scroll", fn);
    window.addEventListener("resize", fn2);
    window.addEventListener("orientationChange", fn2);

    setTimeout(function() { fn2(); }, 1000);
}

function initPieScrolls() {
    initPieScroll(document.getElementById('example-pie-1'));
    initPieScroll(document.getElementById('example-pie-2'));
    initPieScroll(document.getElementById('example-pie-3'));
}