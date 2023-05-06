function toggleCollapsed(wrap) {
	const toggle = wrap.classList.contains('collapsed');
	wrap.querySelector('.article-list').style.display = toggle ? 'block' : 'none';
	wrap.classList.toggle('collapsed');
}
function updateFilter(inp) {
	const enabled = document.body.classList.contains('filter-enabled');
	const kw = inp.value;
	document.querySelectorAll('article').forEach((a)=>{
		const htxt = a.querySelector('.act-link').innerText;
		const d = a.querySelector('.description');
		const dtxt = d ? d.innerText : '';
		const hidden = enabled && kw.length>0 && !htxt.match(kw) && !dtxt.match(kw);
		a.style.display = hidden ? 'none' : 'flex';
	});
}
window.addEventListener('load', ()=>{
	document.querySelectorAll('.year-wrap .num-items').forEach((btn,ix)=>{
		const wrap = btn.closest('.year-wrap');
		btn.addEventListener('click', ()=>{
			toggleCollapsed(wrap);
			btn.scrollIntoView();
		});
		if ( ix > 0 ) toggleCollapsed(wrap);
	});
	document.querySelectorAll('.filter-on, .filter-off').forEach((btn)=>{
		btn.addEventListener('click', ()=>{
			document.body.classList.toggle('filter-enabled');
			updateFilter(document.querySelector('.filter-input'));
		});
	});
	document.querySelectorAll('.filter-input').forEach((inp)=>{
		inp.addEventListener('keyup', ()=>{
			updateFilter(inp);
		});
	});
});
