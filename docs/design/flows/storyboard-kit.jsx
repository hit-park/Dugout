/* Dugout Storyboard Kit — annotation primitives & layout helpers
   Loaded AFTER dugout-screens.jsx. Uses C tokens from screens.
   Provides: ScreenWithNotes, AnnoCard, StateGallery, FlowMap, CoverHeader, Legend, StateRow
*/

/* ─── annotation tokens (cream/green) ─── */
const TS = {
  brand: C.p500, brandSurface: C.p50,
  success: C.success, warning: C.warning, danger: C.danger,
  ai: '#7C3AED', aiSurface: 'rgba(124,58,237,0.10)',
  copy: C.s500, copySurface: C.s100,
  bg: C.c50, paper: C.c0,
  border: C.c200, borderSubtle: C.c100,
  text: C.c900, text2: C.c700, text3: C.c500, text4: C.c400,
};

/* ─── annotation primitives ─── */
function AnnoTag({ color = TS.brand, children }) {
  return <span style={{ padding:'2px 8px', borderRadius:6, background:`${color}1F`, color, fontSize:11, fontWeight:700, fontFamily:'JetBrains Mono,SF Mono,monospace' }}>{children}</span>;
}

function AnnoRow({ label, color, children }) {
  return (
    <div style={{ display:'flex', gap:10, alignItems:'flex-start' }}>
      <span style={{ fontSize:10, fontWeight:700, color, letterSpacing:0.8, textTransform:'uppercase', minWidth:54, paddingTop:2, fontFamily:'JetBrains Mono,monospace' }}>{label}</span>
      <span style={{ fontSize:12, color:TS.text2, lineHeight:1.55, flex:1 }}>{children}</span>
    </div>
  );
}

function AnnoCard({ id, screen, role, action, transition, copy, edge, api, components }) {
  return (
    <div style={{ background:TS.paper, borderRadius:14, padding:'14px 16px', border:`1px solid ${TS.borderSubtle}`, boxShadow:'0 1px 3px rgba(31,30,27,0.05)', display:'flex', flexDirection:'column', gap:10 }}>
      <div style={{ display:'flex', alignItems:'center', gap:8, paddingBottom:8, borderBottom:`1px solid ${TS.borderSubtle}` }}>
        <AnnoTag>{id}</AnnoTag>
        <span style={{ fontSize:14, fontWeight:700, color:TS.text }}>{screen}</span>
      </div>
      {role && <AnnoRow label="역할" color={TS.brand}>{role}</AnnoRow>}
      {action && <AnnoRow label="액션" color={TS.success}>{action}</AnnoRow>}
      {transition && <AnnoRow label="전환" color={TS.ai}>{transition}</AnnoRow>}
      {copy && <AnnoRow label="카피" color={TS.copy}>{copy}</AnnoRow>}
      {components && <AnnoRow label="컴포넌트" color={TS.text3}>{components}</AnnoRow>}
      {api && <AnnoRow label="API" color={TS.text2}>{api}</AnnoRow>}
      {edge && <AnnoRow label="엣지" color={TS.danger}>{edge}</AnnoRow>}
    </div>
  );
}

function ScreenWithNotes(props) {
  const { children, ...anno } = props;
  return (
    <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:14, padding:'8px 0' }}>
      <div style={{ width:320, height:692 }}>{children}</div>
      <div style={{ width:320 }}>
        <AnnoCard {...anno} />
      </div>
    </div>
  );
}

/* ─── state gallery (multi-screen comparison row) ─── */
function StateLabel({ label, tone='brand' }) {
  const map = {
    brand:{c:TS.brand,bg:TS.brandSurface},
    success:{c:TS.success,bg:'rgba(22,163,74,0.12)'},
    warning:{c:TS.warning,bg:'rgba(217,119,6,0.14)'},
    danger:{c:TS.danger,bg:'rgba(220,38,38,0.10)'},
    neutral:{c:TS.text3,bg:C.c100},
  };
  const t = map[tone] || map.brand;
  return <div style={{ fontSize:11, fontWeight:700, color:t.c, background:t.bg, padding:'3px 9px', borderRadius:6, fontFamily:'JetBrains Mono,monospace', alignSelf:'flex-start' }}>{label}</div>;
}

function StateRow({ label, tone, note, children }) {
  return (
    <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
      <StateLabel label={label} tone={tone} />
      {note && <div style={{ fontSize:11, color:TS.text3, fontFamily:'JetBrains Mono,monospace', marginTop:-4 }}>{note}</div>}
      <div>{children}</div>
    </div>
  );
}

function StateGallery({ children }) {
  return <div style={{ display:'flex', gap:32, flexWrap:'wrap' }}>{children}</div>;
}

/* ─── flow map (per-flow diagram) ─── */
function FlowNode({ id, label, tone='brand', size='m' }) {
  const map = {
    brand:{c:TS.brand,bg:TS.brandSurface},
    success:{c:TS.success,bg:'rgba(22,163,74,0.14)'},
    warning:{c:TS.warning,bg:'rgba(217,119,6,0.14)'},
    danger:{c:TS.danger,bg:'rgba(220,38,38,0.10)'},
    neutral:{c:TS.text2,bg:C.c100},
    ai:{c:TS.ai,bg:TS.aiSurface},
  };
  const t = map[tone] || map.brand;
  return (
    <span style={{ display:'inline-flex', flexDirection:'column', alignItems:'flex-start', gap:2, padding:size==='s'?'7px 11px':'9px 13px', borderRadius:9, background:t.bg, color:t.c, fontWeight:700, fontFamily:'JetBrains Mono,monospace', fontSize:size==='s'?11:12, lineHeight:1.2 }}>
      <span style={{ opacity:0.75, fontSize:9, letterSpacing:0.5 }}>{id}</span>
      <span>{label}</span>
    </span>
  );
}

function FlowArrow({ label }) {
  return (
    <span style={{ display:'inline-flex', alignItems:'center', gap:4, color:TS.text3, fontFamily:'JetBrains Mono,monospace', fontSize:10 }}>
      {label && <em style={{ fontStyle:'normal', color:TS.text3 }}>{label}</em>}
      <span>→</span>
    </span>
  );
}

function FlowMap({ title, rows }) {
  return (
    <div style={{ background:TS.paper, borderRadius:18, padding:'24px 28px', border:`1px solid ${TS.borderSubtle}` }}>
      <div style={{ fontSize:11, fontWeight:700, color:TS.text3, letterSpacing:1.2, textTransform:'uppercase', marginBottom:14, fontFamily:'JetBrains Mono,monospace' }}>{title}</div>
      <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
        {rows.map((row,i)=>(
          <div key={i} style={{ display:'flex', alignItems:'center', flexWrap:'wrap', gap:6 }}>
            {row.indent && <span style={{ width:row.indent*48, color:TS.text4, fontFamily:'JetBrains Mono,monospace' }}>{row.branch||''}</span>}
            {row.nodes.map((n,j)=>(
              <React.Fragment key={j}>
                {j>0 && <FlowArrow label={n.via} />}
                <FlowNode {...n} />
              </React.Fragment>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

/* ─── cover + legend ─── */
function CoverHeader({ flowNo, flowName, title, summary, stats }) {
  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      <div style={{ fontSize:13, fontWeight:700, color:TS.brand, letterSpacing:2, textTransform:'uppercase', fontFamily:'JetBrains Mono,monospace' }}>
        Dugout · {flowNo} {flowName} Flow
      </div>
      <h1 style={{ fontSize:52, fontWeight:800, letterSpacing:-1.5, lineHeight:1.05, color:TS.text }}>{title}</h1>
      <p style={{ fontSize:16, color:TS.text2, maxWidth:760, lineHeight:1.6 }}>{summary}</p>
      {stats && (
        <div style={{ display:'grid', gridTemplateColumns:`repeat(${Math.min(stats.length,4)},1fr)`, gap:12, marginTop:8 }}>
          {stats.map((s,i)=>(
            <div key={i} style={{ background:TS.paper, borderRadius:14, padding:'16px 18px', border:`1px solid ${TS.borderSubtle}` }}>
              <div style={{ fontSize:28, fontWeight:800, color:TS.brand, letterSpacing:-0.5, fontFamily:'JetBrains Mono,monospace' }}>{s.num}</div>
              <div style={{ fontSize:12, color:TS.text2, marginTop:4, lineHeight:1.4 }}>{s.label}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function Legend() {
  return (
    <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
      <AnnoTag color={TS.brand}>역할 · 화면의 의도</AnnoTag>
      <AnnoTag color={TS.success}>액션 · 사용자 행동</AnnoTag>
      <AnnoTag color={TS.ai}>전환 · 다음 화면 · 모션</AnnoTag>
      <AnnoTag color={TS.copy}>카피 · 마이크로카피</AnnoTag>
      <AnnoTag color={TS.danger}>엣지 · 예외 처리</AnnoTag>
    </div>
  );
}

/* ─── nav (back to index) ─── */
function FlowNav({ current, items }) {
  return (
    <div style={{ position:'fixed', top:16, right:16, zIndex:9999, display:'flex', gap:6, padding:8, background:TS.paper, borderRadius:12, border:`1px solid ${TS.border}`, boxShadow:'0 4px 16px rgba(31,30,27,0.08)' }}>
      <a href="index.html" style={{ padding:'6px 10px', borderRadius:8, fontSize:11, fontWeight:700, color:TS.text3, textDecoration:'none', fontFamily:'JetBrains Mono,monospace' }}>← INDEX</a>
      {items.map(it => (
        <a key={it.href} href={it.href} style={{
          padding:'6px 10px', borderRadius:8, fontSize:11, fontWeight:700,
          background: it.href===current ? TS.brand : 'transparent',
          color: it.href===current ? '#fff' : TS.text2,
          textDecoration:'none', fontFamily:'JetBrains Mono,monospace'
        }}>{it.label}</a>
      ))}
    </div>
  );
}

const FLOW_NAV = [
  { href:'auth.html',   label:'AUTH' },
  { href:'home.html',   label:'HOME' },
  { href:'team.html',   label:'TEAM' },
  { href:'match.html',  label:'MATCH' },
  { href:'my.html',     label:'MY' },
  { href:'states.html', label:'STATES' },
];

Object.assign(window, {
  TS, AnnoTag, AnnoRow, AnnoCard, ScreenWithNotes,
  StateLabel, StateRow, StateGallery,
  FlowNode, FlowArrow, FlowMap,
  CoverHeader, Legend, FlowNav, FLOW_NAV,
});
