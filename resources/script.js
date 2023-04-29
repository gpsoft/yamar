function toggleCollapsed(wrap) {
	const toggle = wrap.classList.contains('collapsed');
	wrap.querySelectorAll('article').forEach(a=>{
		a.style.display = toggle ? 'block' : 'none';
	});
	wrap.classList.toggle('collapsed');
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
});
