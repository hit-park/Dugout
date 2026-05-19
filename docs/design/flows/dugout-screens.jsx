/* Dugout Flow Screens — shared component library
   All screens from Dugout Flow v3 reorganised + exported to window.
   Loaded BEFORE storyboard-kit.jsx and per-flow Storyboard HTMLs.
   Tokens: Dugout Design Spec (Green #2D8A4E · Cream BG #FAF7F4)
*/

const { useState } = React;

/* ─── tokens ─── */
const C = {
  p50:'#E8F5EE', p100:'#C8E6D2', p200:'#9FD3B0', p300:'#73BE8B',
  p500:'#2D8A4E', p600:'#236F40', p700:'#1A5532', p900:'#0B2917',
  s50:'#FDF1E8', s100:'#FADBC4', s300:'#F0B083', s500:'#D27640', s700:'#974D24',
  c0:'#FFFFFF', c50:'#FAF7F4', c100:'#F1ECE5', c200:'#E0DAD0',
  c300:'#C2BBAF', c400:'#9A9389', c500:'#73706A', c700:'#46443F', c900:'#1F1E1B',
  success:'#16A34A', warning:'#D97706', danger:'#DC2626', info:'#2563EB',
};

/* one-time CSS injection */
if (typeof document !== 'undefined' && !document.getElementById('dugout-screen-styles')) {
  const s = document.createElement('style');
  s.id = 'dugout-screen-styles';
  s.textContent = `
    .dgs-scroll { overflow-y:auto; overflow-x:hidden; scrollbar-width:none; }
    .dgs-scroll::-webkit-scrollbar { display:none; }
    .dgs-device { width:320px; height:692px; border-radius:36px; background:#1F1E1B; padding:8px;
      box-shadow: 0 8px 24px rgba(31,30,27,0.10), inset 0 0 0 1px rgba(255,255,255,0.04); }
    .dgs-screen { width:100%; height:100%; border-radius:30px; overflow:hidden; position:relative;
      background:#FAF7F4; display:flex; flex-direction:column; font-family:'Pretendard',-apple-system,sans-serif; color:#1F1E1B; }
    .dgs-sb { height:36px; flex-shrink:0; display:flex; align-items:center; justify-content:space-between; padding:0 22px; font-size:13px; font-weight:600; color:#1F1E1B; }
    .dgs-tb { height:64px; padding-bottom:14px; flex-shrink:0; background:rgba(255,255,255,0.92); backdrop-filter:blur(10px); border-top:1px solid #E0DAD0; display:grid; grid-template-columns:repeat(5,1fr); }
    .dgs-ti { display:flex; flex-direction:column; align-items:center; justify-content:center; gap:2px; font-size:10px; color:#9A9389; }
    .dgs-ti.active { color:#2D8A4E; }
    .dgs-nb { display:flex; align-items:center; justify-content:space-between; padding:8px 16px; min-height:44px; flex-shrink:0; }
    .dgs-nb.large { padding:8px 16px 12px; flex-direction:column; align-items:flex-start; }
    .dgs-nib { width:32px; height:32px; display:inline-flex; align-items:center; justify-content:center; color:#46443F; font-size:18px; background:none; border:none; cursor:pointer; }
    .dgs-card { background:#FFFFFF; border-radius:14px; padding:12px; box-shadow:0 1px 2px rgba(31,30,27,0.04); }
    .dgs-btn-p { background:#2D8A4E; color:#fff; font-weight:600; padding:12px 16px; border-radius:10px; text-align:center; border:none; cursor:pointer; font-size:15px; width:100%; font-family:inherit; }
    .dgs-btn-s { background:#fff; color:#2D8A4E; font-weight:600; border:1.5px solid #2D8A4E; padding:12px 16px; border-radius:10px; text-align:center; cursor:pointer; font-size:15px; width:100%; font-family:inherit; }
    .dgs-btn-g { color:#2D8A4E; font-weight:500; padding:8px 12px; background:none; border:none; cursor:pointer; font-size:14px; font-family:inherit; }
    .dgs-bdg { display:inline-flex; align-items:center; gap:4px; padding:2px 8px; border-radius:9999px; font-size:11px; font-weight:600; }
    @keyframes dgs-spin { to { transform: rotate(360deg) } }
  `;
  document.head.appendChild(s);
}

/* ─── atoms ─── */
function StatusBar() {
  return <div className="dgs-sb"><span>9:41</span><span style={{fontFamily:'JetBrains Mono,monospace', letterSpacing:1, opacity:0.55}}>·····</span></div>;
}
function TabBar({ active }) {
  const items = [{label:'홈',icon:'🏠'},{label:'일정',icon:'📅'},{label:'매칭',icon:'🤝'},{label:'팀',icon:'⚾'},{label:'마이',icon:'👤'}];
  return (
    <div className="dgs-tb">
      {items.map((it,i)=>(
        <div key={i} className={`dgs-ti ${active===i?'active':''}`}>
          <div style={{fontSize:18, lineHeight:1, filter: active===i?'none':'grayscale(1) opacity(0.55)'}}>{it.icon}</div>
          <span style={{fontWeight: active===i?600:400}}>{it.label}</span>
        </div>
      ))}
    </div>
  );
}
function NavBar({ back, title, right, large }) {
  if (large) return (
    <div className="dgs-nb large">
      <div style={{width:'100%', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
        <div style={{width:32}} />
        {right || <button className="dgs-nib">🔔</button>}
      </div>
      <div style={{fontSize:26, fontWeight:700, color:'#1F1E1B', marginTop:6, letterSpacing:'-0.5px'}}>{title}</div>
    </div>
  );
  return (
    <div className="dgs-nb">
      {back ? <button className="dgs-nib" style={{fontSize:22, marginLeft:-4}}>‹</button> : <div style={{width:32}} />}
      <span style={{fontSize:16, fontWeight:600}}>{title}</span>
      {right || <div style={{width:32}} />}
    </div>
  );
}
function Badge({ children, variant='n' }) {
  const m = {
    p:{ bg:C.p50, color:C.p600 }, s:{ bg:C.s100, color:C.s700 }, n:{ bg:C.c100, color:C.c700 },
    success:{ bg:'rgba(22,163,74,0.12)', color:C.success },
    warning:{ bg:'rgba(217,119,6,0.14)', color:C.warning },
    danger:{ bg:'rgba(220,38,38,0.10)', color:C.danger },
  };
  const v = m[variant] || m.n;
  return <span className="dgs-bdg" style={{background:v.bg, color:v.color}}>{children}</span>;
}

/* ═══ AUTH ═══ */
function S_Splash() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <div style={{flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', background:C.p500}}>
      <div style={{fontSize:72, marginBottom:14}}>⚾</div>
      <div style={{fontSize:30, fontWeight:700, color:'#fff', letterSpacing:-0.5}}>Dugout</div>
      <div style={{fontSize:13, color:'rgba(255,255,255,0.78)', marginTop:8}}>야구팀 운영의 모든 것</div>
    </div>
  </div></div>);
}
function S_Login() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb"><button className="dgs-nib" style={{color:C.c500, fontSize:18}}>✕</button><span style={{fontSize:16, fontWeight:600}}>로그인</span><div style={{width:32}} /></div>
    <div style={{padding:'24px 20px 8px', display:'flex', flexDirection:'column', alignItems:'center', gap:6}}>
      <div style={{fontSize:56, marginTop:12}}>⚾</div>
      <div style={{fontSize:20, fontWeight:700, marginTop:4}}>Dugout에 오신 걸 환영합니다</div>
      <div style={{fontSize:13, color:C.c500, textAlign:'center'}}>간편 로그인으로 시작하세요</div>
    </div>
    <div style={{padding:'20px 20px 0', display:'flex', flexDirection:'column', gap:10}}>
      <button style={{padding:'14px', borderRadius:12, background:'#FEE500', color:'#3C1E1E', fontWeight:700, border:'none', cursor:'pointer', fontSize:15}}>카카오로 시작하기</button>
      <button style={{padding:'14px', borderRadius:12, background:'#03C75A', color:'#fff', fontWeight:700, border:'none', cursor:'pointer', fontSize:15}}>네이버로 시작하기</button>
      <button style={{padding:'14px', borderRadius:12, background:C.c900, color:'#fff', fontWeight:600, border:'none', cursor:'pointer', fontSize:15}}>Apple로 시작하기</button>
      <button style={{padding:'14px', borderRadius:12, background:'#fff', color:C.c900, fontWeight:600, border:'1px solid '+C.c200, cursor:'pointer', fontSize:15}}>Google로 시작하기</button>
    </div>
    <div style={{flex:1}} />
    <div style={{padding:'16px 20px 22px', textAlign:'center'}}><button className="dgs-btn-g" style={{fontSize:13}}>로그인 없이 둘러보기</button></div>
  </div></div>);
}
function S_OnboardNickname() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div style={{padding:'4px 20px 0'}}>
      <div style={{fontSize:11, color:C.c500, fontFamily:'JetBrains Mono,monospace'}}>1 / 3</div>
      <div style={{height:4, background:C.c200, borderRadius:2, marginTop:8}}><div style={{height:'100%', width:'33%', background:C.p500, borderRadius:2}} /></div>
      <div style={{fontSize:22, fontWeight:700, marginTop:24, letterSpacing:-0.3}}>닉네임을 알려주세요</div>
      <div style={{fontSize:13, color:C.c500, marginTop:4}}>팀원들에게 보일 이름이에요</div>
      <div style={{marginTop:24}}>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>닉네임</label>
        <input defaultValue="김주장" style={{width:'100%', marginTop:6, padding:'12px 14px', borderRadius:10, background:C.c50, border:'1.5px solid '+C.p500, fontSize:15, outline:'none'}} />
        <div style={{fontSize:11, color:C.p500, marginTop:6}}>✓ 사용 가능한 닉네임이에요</div>
      </div>
      <div style={{marginTop:18}}>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>등번호 (선택)</label>
        <input placeholder="예) 7" style={{width:'100%', marginTop:6, padding:'12px 14px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:15, outline:'none'}} />
      </div>
    </div>
    <div style={{flex:1}} />
    <div style={{padding:'12px 20px 20px'}}><button className="dgs-btn-p">다음</button></div>
  </div></div>);
}
function S_OnboardPosition() {
  const [main, setMain] = useState('1B');
  const [subs, setSubs] = useState(['3B','DH']);
  const positions = ['P','C','1B','2B','3B','SS','LF','CF','RF'];
  const toggleSub = p => setSubs(s => s.includes(p) ? s.filter(x=>x!==p) : [...s,p]);
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div style={{padding:'4px 20px 0', flex:1, overflowY:'auto'}}>
      <div style={{fontSize:11, color:C.c500, fontFamily:'JetBrains Mono,monospace'}}>2 / 3</div>
      <div style={{height:4, background:C.c200, borderRadius:2, marginTop:8}}><div style={{height:'100%', width:'66%', background:C.p500, borderRadius:2}} /></div>
      <div style={{fontSize:22, fontWeight:700, marginTop:24, letterSpacing:-0.3}}>포지션을 알려주세요</div>
      <div style={{fontSize:13, color:C.c500, marginTop:4}}>라인업 추천에 사용돼요</div>
      <div style={{fontSize:11, fontWeight:600, color:C.c700, marginTop:20, marginBottom:8}}>주 포지션</div>
      <div style={{display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8}}>
        {positions.map(p => (
          <button key={p} onClick={()=>setMain(p)} style={{padding:'12px 0', borderRadius:12, background: main===p ? C.p500 : '#fff', color: main===p ? '#fff' : C.c900, fontSize:14, fontWeight:600, border:'none', cursor:'pointer', boxShadow:'0 1px 2px rgba(31,30,27,0.04)'}}>{p}</button>
        ))}
      </div>
      <div style={{fontSize:11, fontWeight:600, color:C.c700, marginTop:18, marginBottom:8}}>서브 포지션 (복수)</div>
      <div style={{display:'flex', flexWrap:'wrap', gap:6}}>
        {['P','C','2B','3B','SS','LF','CF','RF','DH'].filter(p=>p!==main).map(p => (
          <button key={p} onClick={()=>toggleSub(p)} className="dgs-bdg" style={{background: subs.includes(p) ? C.p50 : C.c100, color: subs.includes(p) ? C.p600 : C.c500, padding:'5px 10px', border:'none', cursor:'pointer'}}>
            {subs.includes(p) ? '✓ ' : '+ '}{p}
          </button>
        ))}
      </div>
    </div>
    <div style={{padding:'12px 20px 20px'}}><button className="dgs-btn-p">다음</button></div>
  </div></div>);
}
function S_OnboardChoose() {
  const items = [
    { icon:'⚾', title:'팀 만들기', sub:'새 팀의 주장이 되어 멤버를 초대합니다' },
    { icon:'🤝', title:'팀 참가', sub:'초대 코드로 기존 팀에 합류합니다' },
    { icon:'🎯', title:'용병으로 시작', sub:'고정 팀 없이 매칭으로 경기에 참가합니다' },
  ];
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div style={{padding:'4px 20px 0'}}>
      <div style={{fontSize:11, color:C.c500, fontFamily:'JetBrains Mono,monospace'}}>3 / 3</div>
      <div style={{height:4, background:C.c200, borderRadius:2, marginTop:8}}><div style={{height:'100%', width:'100%', background:C.p500, borderRadius:2}} /></div>
      <div style={{fontSize:22, fontWeight:700, marginTop:24, letterSpacing:-0.3}}>어떻게 시작하시겠어요?</div>
      <div style={{display:'flex', flexDirection:'column', gap:10, marginTop:20}}>
        {items.map((it,i)=>(
          <div key={i} className="dgs-card" style={{display:'flex', alignItems:'flex-start', gap:12, cursor:'pointer'}}>
            <div style={{fontSize:28}}>{it.icon}</div>
            <div style={{flex:1}}>
              <div style={{fontSize:15, fontWeight:600}}>{it.title}</div>
              <div style={{fontSize:12, color:C.c500, marginTop:3, lineHeight:1.4}}>{it.sub}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
    <div style={{flex:1}} />
    <div style={{padding:'12px 20px 20px', textAlign:'center'}}><button className="dgs-btn-g" style={{fontSize:13}}>나중에 결정하기</button></div>
  </div></div>);
}

/* ═══ HOME ═══ */
function S_HomeEmpty() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <NavBar large title="Dugout" />
    <div style={{padding:'12px 20px 0', display:'flex', flexDirection:'column', alignItems:'center', textAlign:'center', flex:1}}>
      <div style={{fontSize:64, marginTop:20, marginBottom:14}}>⚾</div>
      <div style={{fontSize:18, fontWeight:700}}>팀과 함께 시작해요</div>
      <div style={{fontSize:13, color:C.c500, marginTop:6}}>팀을 만들거나 초대 코드로 참여하세요</div>
      <div style={{width:'100%', marginTop:22, display:'flex', flexDirection:'column', gap:8}}>
        <button className="dgs-btn-p">팀 만들기</button>
        <button className="dgs-btn-s">초대 코드로 참여</button>
      </div>
      <button className="dgs-btn-g" style={{marginTop:10, fontSize:13}}>로그인 없이 둘러보기</button>
    </div>
    <TabBar active={0} />
  </div></div>);
}
function S_HomeDashboard() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb large">
      <div style={{width:'100%', display:'flex', justifyContent:'space-between', alignItems:'flex-start'}}>
        <div>
          <div style={{fontSize:12, color:C.c500}}>안녕하세요</div>
          <div style={{fontSize:26, fontWeight:700, color:C.c900, marginTop:2, letterSpacing:'-0.5px'}}>김주장 님</div>
        </div>
        <div style={{position:'relative', marginTop:6}}>
          <button className="dgs-nib">🔔</button>
          <span style={{position:'absolute', top:4, right:4, width:8, height:8, background:C.danger, borderRadius:4}} />
        </div>
      </div>
    </div>
    <div className="dgs-scroll" style={{flex:1, padding:'0 20px', display:'flex', flexDirection:'column', gap:10}}>
      <div style={{display:'flex', gap:6, overflowX:'auto', paddingBottom:4}}>
        <span className="dgs-bdg" style={{background:C.p500, color:'#fff', padding:'6px 12px'}}>⚾ 라이거스</span>
        <span className="dgs-bdg" style={{background:'#fff', color:C.c700, border:'1px solid '+C.c200, padding:'6px 12px'}}>FC 청춘</span>
        <span className="dgs-bdg" style={{background:'#fff', color:C.c400, border:'1px dashed '+C.c200, padding:'6px 12px'}}>+ 팀 추가</span>
      </div>
      <div className="dgs-card" style={{padding:14}}>
        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
          <Badge variant="s">D-3</Badge><span style={{fontSize:11, color:C.c500}}>토 14:00</span>
        </div>
        <div style={{fontSize:18, fontWeight:700, marginTop:8}}>vs 청룡 BC</div>
        <div style={{fontSize:11, color:C.c500, marginTop:2}}>📍 잠실 한강 야구장</div>
        <div style={{marginTop:12, paddingTop:10, borderTop:'1px solid '+C.c100, display:'flex', justifyContent:'space-between', alignItems:'center'}}>
          <span style={{fontSize:11, color:C.c500}}>내 응답</span><Badge variant="success">✓ 참가</Badge>
        </div>
      </div>
      <div className="dgs-card" style={{background:C.p50, border:'1px solid '+C.p100, padding:14}}>
        <div style={{display:'flex', gap:10, alignItems:'flex-start'}}>
          <span style={{fontSize:18}}>🤖</span>
          <div style={{flex:1}}>
            <div style={{fontSize:11, fontWeight:700, color:C.p600}}>AI 출석 예측</div>
            <div style={{fontSize:14, fontWeight:700, marginTop:2}}>예상 14~16명 참가</div>
            <div style={{fontSize:11, color:C.c500, marginTop:3}}>미응답 3명 중 2명은 참가 가능성 ↑</div>
          </div>
        </div>
      </div>
      <div className="dgs-card">
        <div style={{fontSize:11, fontWeight:600, color:C.c500}}>팀 공지</div>
        <div style={{fontSize:13, fontWeight:500, marginTop:4}}>이번 달 회비 25,000원 입금 부탁드립니다</div>
        <div style={{fontSize:11, color:C.c400, marginTop:3}}>2일 전</div>
      </div>
      <div style={{height:8}} />
    </div>
    <TabBar active={0} />
  </div></div>);
}

/* ═══ TEAM ═══ */
function S_TeamList() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <NavBar title="내 팀" right={<button className="dgs-nib">🔔</button>} />
    <div className="dgs-scroll" style={{flex:1, padding:'0 20px', display:'flex', flexDirection:'column', gap:10}}>
      <div className="dgs-card">
        <div style={{display:'flex', alignItems:'center', gap:12}}>
          <div style={{width:48, height:48, borderRadius:24, background:C.p100, display:'flex', alignItems:'center', justifyContent:'center', fontSize:22}}>⚾</div>
          <div style={{flex:1}}>
            <div style={{fontSize:14, fontWeight:600}}>수원 라이거스</div>
            <div style={{fontSize:11, color:C.c500, marginTop:2}}>서울 강남 · 2부</div>
          </div>
          <Badge variant="s">주장</Badge>
        </div>
        <div style={{display:'flex', gap:12, marginTop:10, paddingTop:10, borderTop:'1px solid '+C.c100, fontSize:11, color:C.c500}}>
          <span>👥 18명</span><span>📅 다음 경기 D-3</span>
        </div>
      </div>
      <div className="dgs-card">
        <div style={{display:'flex', alignItems:'center', gap:12}}>
          <div style={{width:48, height:48, borderRadius:24, background:C.s100, display:'flex', alignItems:'center', justifyContent:'center', fontSize:22}}>🦁</div>
          <div style={{flex:1}}>
            <div style={{fontSize:14, fontWeight:600}}>FC 청춘</div>
            <div style={{fontSize:11, color:C.c500, marginTop:2}}>서울 송파 · 3부</div>
          </div>
          <Badge variant="n">일반</Badge>
        </div>
        <div style={{display:'flex', gap:12, marginTop:10, paddingTop:10, borderTop:'1px solid '+C.c100, fontSize:11, color:C.c500}}>
          <span>👥 22명</span>
        </div>
      </div>
      <div style={{flex:1}} />
      <button className="dgs-btn-p">+ 팀 만들기</button>
      <button className="dgs-btn-g" style={{fontSize:13}}>초대 코드로 참여</button>
      <div style={{height:8}} />
    </div>
    <TabBar active={3} />
  </div></div>);
}
function S_TeamCreate() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb" style={{borderBottom:'1px solid '+C.c100}}>
      <button className="dgs-btn-g" style={{color:C.c500, padding:0, fontSize:14}}>취소</button>
      <span style={{fontSize:16, fontWeight:600}}>팀 만들기</span>
      <button className="dgs-btn-g" style={{padding:0, fontWeight:600, fontSize:14}}>저장</button>
    </div>
    <div className="dgs-scroll" style={{padding:'18px 20px', display:'flex', flexDirection:'column', gap:14, flex:1}}>
      <div>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>팀 이름</label>
        <input defaultValue="수원 라이거스" style={{width:'100%', marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:14, outline:'none'}} />
      </div>
      <div>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>지역</label>
        <input defaultValue="서울 강남" style={{width:'100%', marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:14, outline:'none'}} />
      </div>
      <div>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>부수</label>
        <div style={{display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:6, marginTop:6}}>
          {['1부','2부','3부','4부'].map((d,i)=>(<button key={d} style={{padding:'9px 0', borderRadius:10, background:i===1?C.p500:'#fff', color:i===1?'#fff':C.c900, border:i===1?'none':'1px solid '+C.c200, fontWeight:i===1?600:400, fontSize:13, cursor:'pointer'}}>{d}</button>))}
        </div>
      </div>
      <div>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>활동 요일</label>
        <div style={{display:'grid', gridTemplateColumns:'repeat(7,1fr)', gap:4, marginTop:6}}>
          {['월','화','수','목','금','토','일'].map((d,i)=>(<button key={d} style={{padding:'9px 0', borderRadius:10, background:i===5?C.p500:'#fff', color:i===5?'#fff':C.c900, border:i===5?'none':'1px solid '+C.c200, fontSize:12, cursor:'pointer'}}>{d}</button>))}
        </div>
      </div>
      <div>
        <label style={{fontSize:11, fontWeight:600, color:C.c700}}>라인업 모드</label>
        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:6, marginTop:6}}>
          <button style={{padding:'10px 0', borderRadius:10, background:C.p500, color:'#fff', border:'none', fontWeight:600, fontSize:13, cursor:'pointer'}}>균등 출전</button>
          <button style={{padding:'10px 0', borderRadius:10, background:'#fff', color:C.c900, border:'1px solid '+C.c200, fontSize:13, cursor:'pointer'}}>실력 우선</button>
        </div>
      </div>
    </div>
  </div></div>);
}
function S_TeamJoin() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb" style={{borderBottom:'1px solid '+C.c100}}>
      <button className="dgs-nib" style={{fontSize:22, marginLeft:-4}}>‹</button>
      <span style={{fontSize:16, fontWeight:600}}>팀 참여</span><div style={{width:32}} />
    </div>
    <div style={{padding:'36px 24px 0', display:'flex', flexDirection:'column', alignItems:'center', textAlign:'center', flex:1}}>
      <div style={{fontSize:56, marginBottom:14}}>🎫</div>
      <div style={{fontSize:18, fontWeight:700}}>초대 코드 입력</div>
      <div style={{fontSize:12, color:C.c500, marginTop:6}}>주장에게 받은 6자리 코드를 입력해주세요</div>
      <div style={{marginTop:28, display:'flex', gap:6, fontFamily:'JetBrains Mono,monospace'}}>
        {['A','B','C','1','2','3'].map((ch,i)=>(<div key={i} style={{width:38, height:48, borderRadius:10, border:`${i===0?2:1}px solid ${i===0?C.p500:C.c200}`, display:'flex', alignItems:'center', justifyContent:'center', fontSize:18, fontWeight:700, background:'#fff'}}>{ch}</div>))}
      </div>
      <button className="dgs-btn-g" style={{marginTop:18, fontSize:13}}>📷 QR 코드 스캔하기</button>
    </div>
    <div style={{padding:'12px 20px 22px'}}><button className="dgs-btn-p">참여하기</button></div>
  </div></div>);
}
function S_TeamDetail() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <NavBar back title="수원 라이거스" right={<button className="dgs-nib">⋯</button>} />
    <div className="dgs-scroll" style={{flex:1, padding:'0 20px', display:'flex', flexDirection:'column', gap:10}}>
      <div className="dgs-card" style={{display:'flex', alignItems:'center', gap:12}}>
        <div style={{width:54, height:54, borderRadius:27, background:C.p100, display:'flex', alignItems:'center', justifyContent:'center', fontSize:22}}>⚾</div>
        <div style={{flex:1}}>
          <div style={{fontSize:15, fontWeight:700}}>수원 라이거스</div>
          <div style={{fontSize:11, color:C.c500, marginTop:2}}>서울 강남 · 2부 · 매주 토요일</div>
        </div>
        <Badge variant="s">주장</Badge>
      </div>
      <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:8}}>
        <button className="dgs-card" style={{display:'flex', alignItems:'center', justifyContent:'center', gap:4, border:'none', fontSize:13, fontWeight:600, cursor:'pointer'}}><span>📅</span> 경기 등록</button>
        <button className="dgs-card" style={{display:'flex', alignItems:'center', justifyContent:'center', gap:4, border:'none', fontSize:13, fontWeight:600, cursor:'pointer'}}><span>🎫</span> 초대 코드</button>
        <button className="dgs-card" style={{display:'flex', alignItems:'center', justifyContent:'center', gap:4, border:'none', fontSize:13, fontWeight:600, color:C.c400, cursor:'pointer'}}><span>⚾</span> 라인업</button>
        <button className="dgs-card" style={{display:'flex', alignItems:'center', justifyContent:'center', gap:4, border:'none', fontSize:13, fontWeight:600, color:C.c400, cursor:'pointer'}}><span>💰</span> 회비</button>
      </div>
      <div>
        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'0 4px', marginBottom:8}}>
          <span style={{fontSize:11, fontWeight:600, color:C.c700}}>멤버 (18)</span>
          <button className="dgs-btn-g" style={{fontSize:11, padding:0}}>전체 보기</button>
        </div>
        <div style={{display:'flex', flexDirection:'column', gap:6}}>
          {[{n:'김주장 #7',r:'주장',v:'s'},{n:'이매니저 #14',r:'매니저',v:'p'},{n:'박선수 #22',r:'일반',v:'n'}].map((m,i)=>(
            <div key={i} className="dgs-card" style={{display:'flex', alignItems:'center', gap:8, padding:'10px 12px'}}>
              <div style={{width:28, height:28, borderRadius:14, background:C.c200}} />
              <span style={{fontSize:13, fontWeight:500, flex:1}}>{m.n}</span>
              <Badge variant={m.v}>{m.r}</Badge>
            </div>
          ))}
        </div>
      </div>
      <div style={{height:8}} />
    </div>
  </div></div>);
}
function S_MemberDialog() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb" style={{borderBottom:'1px solid '+C.c100}}>
      <button className="dgs-nib" style={{fontSize:22, marginLeft:-4}}>‹</button>
      <span style={{fontSize:16, fontWeight:600}}>멤버 관리</span><div style={{width:32}} />
    </div>
    <div style={{padding:'0 20px', display:'flex', flexDirection:'column', gap:14, flex:1}}>
      <div className="dgs-card" style={{display:'flex', alignItems:'center', gap:12, marginTop:14}}>
        <div style={{width:48, height:48, borderRadius:24, background:C.c200}} />
        <div style={{flex:1}}>
          <div style={{fontSize:15, fontWeight:700}}>박선수 #22</div>
          <div style={{fontSize:11, color:C.c500, marginTop:2}}>가입일 2024.03.18 · 출석률 86%</div>
        </div>
        <Badge variant="n">일반</Badge>
      </div>
      <div>
        <div style={{fontSize:11, fontWeight:600, color:C.c700, marginBottom:8, padding:'0 4px'}}>권한 변경</div>
        <div style={{background:'#fff', borderRadius:12, boxShadow:'0 1px 2px rgba(31,30,27,0.04)', overflow:'hidden'}}>
          {[{l:'매니저로 변경',sub:'경기·공지·멤버 관리'},{l:'회계로 변경',sub:'회비 관리만 가능'},{l:'일반으로 변경',sub:'기본 권한',active:true}].map((it,i,a)=>(
            <div key={i} style={{padding:'13px 14px', display:'flex', justifyContent:'space-between', alignItems:'center', borderBottom:i<a.length-1?'1px solid '+C.c100:'none', cursor:'pointer'}}>
              <div>
                <div style={{fontSize:14, fontWeight:500}}>{it.l}</div>
                <div style={{fontSize:11, color:C.c500, marginTop:2}}>{it.sub}</div>
              </div>
              {it.active && <span style={{color:C.p500, fontSize:18}}>✓</span>}
            </div>
          ))}
        </div>
      </div>
      <div style={{flex:1}} />
      <div style={{paddingBottom:20}}><button style={{width:'100%', padding:'13px', borderRadius:10, background:'rgba(220,38,38,0.08)', color:C.danger, fontWeight:600, border:'none', cursor:'pointer', fontSize:14}}>팀에서 추방</button></div>
    </div>
  </div></div>);
}

/* ═══ MATCH ═══ */
function S_MatchList() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb"><span style={{fontSize:16, fontWeight:600}}>일정</span><span style={{fontSize:11, color:C.c500}}>전체 팀 ▾</span><button className="dgs-nib">🔔</button></div>
    <div style={{padding:'0 20px', flex:1, overflow:'hidden'}}>
      <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:4, background:C.c100, borderRadius:10, padding:4, marginBottom:12}}>
        <button style={{padding:'6px 0', borderRadius:7, background:'#fff', fontSize:13, fontWeight:600, border:'none', cursor:'pointer'}}>캘린더</button>
        <button style={{padding:'6px 0', borderRadius:7, background:'transparent', fontSize:13, color:C.c500, border:'none', cursor:'pointer'}}>리스트</button>
      </div>
      <div style={{textAlign:'center', fontSize:12, fontWeight:600, marginBottom:6}}>2026년 5월</div>
      <div style={{display:'grid', gridTemplateColumns:'repeat(7,1fr)', gap:2, fontSize:10, color:C.c400, textAlign:'center'}}>
        {['일','월','화','수','목','금','토'].map(d=><span key={d}>{d}</span>)}
      </div>
      <div style={{display:'grid', gridTemplateColumns:'repeat(7,1fr)', gap:2, fontSize:11, marginTop:4}}>
        {[null,null,null,null,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17].map((d,i)=>{
          const hasGame = d===3 || d===17; const isSel = d===10;
          return (<div key={i} style={{aspectRatio:'1', display:'flex', alignItems:'center', justifyContent:'center', position:'relative', borderRadius:isSel?100:0, background:isSel?C.p500:'transparent', color:isSel?'#fff':d?C.c900:C.c400, fontWeight:isSel?600:400}}>{d||'·'}{hasGame && !isSel && <span style={{position:'absolute', bottom:1, width:3, height:3, borderRadius:2, background:C.p500}} />}</div>);
        })}
      </div>
      <div style={{marginTop:14, display:'flex', flexDirection:'column', gap:6}}>
        <div className="dgs-card" style={{padding:10}}>
          <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}><Badge variant="s">D-3 · 토</Badge><span style={{fontSize:11, color:C.c500}}>14:00</span></div>
          <div style={{fontSize:13, fontWeight:700, marginTop:4}}>vs 청룡 BC</div>
          <div style={{fontSize:11, color:C.c500, marginTop:1}}>잠실 한강 야구장</div>
        </div>
        <div className="dgs-card" style={{padding:10}}>
          <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}><Badge variant="n">D-10 · 토</Badge><span style={{fontSize:11, color:C.c500}}>10:00</span></div>
          <div style={{fontSize:13, fontWeight:700, marginTop:4}}>vs 미정</div>
        </div>
      </div>
    </div>
    <div style={{position:'absolute', bottom:80, right:14, width:48, height:48, borderRadius:24, background:C.p500, color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', fontSize:24, boxShadow:'0 4px 12px rgba(45,138,78,0.3)'}}>+</div>
    <TabBar active={1} />
  </div></div>);
}
function S_MatchCreate() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb" style={{borderBottom:'1px solid '+C.c100}}>
      <button className="dgs-btn-g" style={{color:C.c500, padding:0, fontSize:14}}>취소</button>
      <span style={{fontSize:16, fontWeight:600}}>경기 등록</span>
      <button className="dgs-btn-g" style={{padding:0, fontWeight:600, fontSize:14}}>저장</button>
    </div>
    <div className="dgs-scroll" style={{padding:'18px 20px', display:'flex', flexDirection:'column', gap:14, flex:1}}>
      <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:8}}>
        <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>날짜</label><div style={{marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13}}>2026.05.10 (토)</div></div>
        <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>경기 시간</label><div style={{marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13}}>14:00</div></div>
      </div>
      <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>집합 시간 (선택)</label><div style={{marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13}}>13:30</div></div>
      <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>상대팀</label><input defaultValue="청룡 BC" style={{width:'100%', marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13, outline:'none'}} /></div>
      <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>구장</label><input defaultValue="잠실 한강 야구장" style={{width:'100%', marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13, outline:'none'}} /></div>
      <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>출석 투표 마감</label><div style={{marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13, display:'flex', justifyContent:'space-between'}}><span>경기 24시간 전</span><span style={{color:C.c400}}>▾</span></div></div>
      <div><label style={{fontSize:11, fontWeight:600, color:C.c700}}>메모 (선택)</label><textarea placeholder="복장·준비물 안내" rows="2" style={{width:'100%', marginTop:6, padding:'10px 12px', borderRadius:10, background:C.c50, border:'1px solid '+C.c200, fontSize:13, outline:'none', resize:'none'}} /></div>
    </div>
  </div></div>);
}
function S_MatchDetail() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <NavBar back title="D-3 · 라이거스" right={<button className="dgs-nib">⋯</button>} />
    <div className="dgs-scroll" style={{flex:1, padding:'0 20px', display:'flex', flexDirection:'column', gap:10}}>
      <div className="dgs-card">
        <span className="dgs-bdg" style={{background:C.s500, color:'#fff'}}>D-3 · 토 14:00</span>
        <div style={{fontSize:20, fontWeight:700, marginTop:8}}>vs 청룡 BC</div>
        <div style={{fontSize:11, color:C.c500, marginTop:4}}>📍 잠실 한강 야구장</div>
        <div style={{fontSize:11, color:C.c500}}>⏰ 집합 13:30</div>
      </div>
      <div className="dgs-card" style={{background:C.p50}}>
        <div style={{fontSize:11, fontWeight:600, color:C.p600}}>내 응답</div>
        <div style={{marginTop:6, display:'flex', justifyContent:'space-between', alignItems:'center'}}>
          <Badge variant="success">✓ 참가</Badge>
          <button className="dgs-btn-g" style={{fontSize:11, color:C.p700, fontWeight:600, padding:0}}>변경</button>
        </div>
      </div>
      <div className="dgs-card">
        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}><span style={{fontSize:11, fontWeight:600, color:C.c700}}>출석 현황</span><button className="dgs-btn-g" style={{fontSize:11, padding:0}}>전체 보기 ›</button></div>
        <div style={{marginTop:10, display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:6, textAlign:'center'}}>
          <div><div style={{fontSize:20, fontWeight:700, color:C.success}}>12</div><div style={{fontSize:11, color:C.c500}}>참가</div></div>
          <div><div style={{fontSize:20, fontWeight:700, color:C.danger}}>2</div><div style={{fontSize:11, color:C.c500}}>불참</div></div>
          <div><div style={{fontSize:20, fontWeight:700, color:C.c500}}>4</div><div style={{fontSize:11, color:C.c500}}>미응답</div></div>
        </div>
        <div style={{marginTop:10, display:'flex', justifyContent:'center', gap:14, fontSize:11, color:C.c500}}><span>⏰ 늦참 1</span><span>🚪 조퇴 1</span></div>
      </div>
      <div className="dgs-card" style={{background:C.p50, border:'1px solid '+C.p100}}>
        <div style={{display:'flex', gap:10, alignItems:'flex-start'}}>
          <span style={{fontSize:18}}>🤖</span>
          <div><div style={{fontSize:11, fontWeight:700, color:C.p600}}>AI 출석 예측 (v0.5+)</div><div style={{fontSize:13, fontWeight:700, marginTop:2}}>예상 14~16명</div></div>
        </div>
      </div>
      <div style={{height:8}} />
    </div>
    <div style={{padding:'10px 20px 14px'}}><button className="dgs-btn-p">응답 변경</button></div>
  </div></div>);
}
function S_AttVote() {
  const [pick, setPick] = useState('참가');
  const opts = [{v:'참가',icon:'✓',color:C.success,desc:'경기 시작부터 끝까지 참여'},{v:'불참',icon:'✕',color:C.danger,desc:'이번 경기 참여 어려움'},{v:'미정',icon:'?',color:C.c500,desc:'아직 확정되지 않음'}];
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb" style={{borderBottom:'1px solid '+C.c100}}>
      <button className="dgs-nib" style={{fontSize:22, marginLeft:-4}}>‹</button>
      <span style={{fontSize:16, fontWeight:600}}>출석 응답</span><div style={{width:32}} />
    </div>
    <div className="dgs-scroll" style={{padding:'16px 20px 0', display:'flex', flexDirection:'column', gap:12, flex:1}}>
      <div className="dgs-card" style={{background:C.p50, padding:12}}>
        <span className="dgs-bdg" style={{background:C.s500, color:'#fff'}}>D-3 · 토 14:00</span>
        <div style={{fontSize:16, fontWeight:700, marginTop:6}}>vs 청룡 BC</div>
        <div style={{fontSize:11, color:C.c500, marginTop:2}}>📍 잠실 한강 야구장</div>
      </div>
      <div style={{fontSize:11, fontWeight:600, color:C.c700, marginTop:4}}>응답 선택</div>
      <div style={{display:'flex', flexDirection:'column', gap:8}}>
        {opts.map(o => (
          <button key={o.v} onClick={()=>setPick(o.v)} style={{width:'100%', padding:'14px', borderRadius:12, textAlign:'left', background: pick===o.v ? `${o.color}1A` : '#fff', border: pick===o.v ? `2px solid ${o.color}` : '1px solid '+C.c200, display:'flex', alignItems:'center', gap:12, cursor:'pointer', color: pick===o.v ? o.color : C.c900}}>
            <span style={{fontSize:20, width:24, textAlign:'center'}}>{o.icon}</span>
            <div style={{flex:1}}>
              <div style={{fontSize:14, fontWeight:600}}>{o.v}</div>
              <div style={{fontSize:11, color: pick===o.v ? o.color : C.c500, marginTop:2, opacity: pick===o.v ? 0.85 : 1}}>{o.desc}</div>
            </div>
          </button>
        ))}
      </div>
      <div style={{fontSize:11, fontWeight:600, color:C.c700, marginTop:6}}>부분 참여 (선택)</div>
      <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:8}}>
        <button style={{padding:'12px 0', borderRadius:12, background:'#fff', border:'1px solid '+C.c200, display:'flex', alignItems:'center', justifyContent:'center', gap:6, fontSize:13, cursor:'pointer'}}><span>⏰</span><span>늦참</span></button>
        <button style={{padding:'12px 0', borderRadius:12, background:'#fff', border:'1px solid '+C.c200, display:'flex', alignItems:'center', justifyContent:'center', gap:6, fontSize:13, cursor:'pointer'}}><span>🚪</span><span>조퇴</span></button>
      </div>
      <div>
        <div style={{fontSize:11, fontWeight:600, color:C.c700, marginBottom:6}}>사유 (선택)</div>
        <textarea placeholder="팀에게 알려주세요" rows="3" style={{width:'100%', padding:'10px 12px', borderRadius:10, background:'#fff', border:'1px solid '+C.c200, fontSize:13, outline:'none', resize:'none'}} />
      </div>
    </div>
    <div style={{padding:'12px 20px 22px'}}><button className="dgs-btn-p">응답하기</button></div>
  </div></div>);
}
function S_AttSummary() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <NavBar back title="출석 현황" />
    <div className="dgs-scroll" style={{flex:1, padding:'0 20px', display:'flex', flexDirection:'column', gap:10}}>
      <div className="dgs-card">
        <div style={{display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:6, textAlign:'center'}}>
          <div><div style={{fontSize:22, fontWeight:700, color:C.success}}>12</div><div style={{fontSize:11, color:C.c500}}>참가</div></div>
          <div><div style={{fontSize:22, fontWeight:700, color:C.danger}}>2</div><div style={{fontSize:11, color:C.c500}}>불참</div></div>
          <div><div style={{fontSize:22, fontWeight:700, color:C.c500}}>4</div><div style={{fontSize:11, color:C.c500}}>미응답</div></div>
        </div>
        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:6, marginTop:10, fontSize:11, textAlign:'center'}}>
          <div style={{padding:8, background:'rgba(217,119,6,0.10)', color:C.warning, borderRadius:8}}>⏰ 늦참 1</div>
          <div style={{padding:8, background:C.s100, color:C.s700, borderRadius:8}}>🚪 조퇴 1</div>
        </div>
      </div>
      <div style={{display:'flex', gap:4, background:C.c100, borderRadius:8, padding:4, fontSize:11}}>
        {['전체','참가','불참','미응답'].map((f,i)=>(<button key={f} style={{padding:'5px 10px', borderRadius:6, background:i===0?'#fff':'transparent', color:i===0?C.c900:C.c500, fontWeight:i===0?600:400, border:'none', cursor:'pointer', flex:1}}>{f}</button>))}
      </div>
      <div style={{display:'flex', flexDirection:'column', gap:6}}>
        <div className="dgs-card" style={{display:'flex', alignItems:'center', gap:8, padding:'10px 12px'}}>
          <div style={{width:26, height:26, borderRadius:13, background:C.c200}} />
          <span style={{fontSize:13, fontWeight:500, flex:1}}>박선수 #22</span><Badge variant="success">✓</Badge>
        </div>
        <div className="dgs-card" style={{display:'flex', alignItems:'center', gap:8, padding:'10px 12px'}}>
          <div style={{width:26, height:26, borderRadius:13, background:C.c200}} />
          <div style={{flex:1}}><div style={{fontSize:13, fontWeight:500}}>최선수 #11</div><div style={{fontSize:11, color:C.c500, marginTop:1}}>"늦게 합류 가능"</div></div>
          <Badge variant="warning">⏰</Badge>
        </div>
        <div className="dgs-card" style={{display:'flex', alignItems:'center', gap:8, padding:'10px 12px'}}>
          <div style={{width:26, height:26, borderRadius:13, background:C.c200}} />
          <span style={{fontSize:13, color:C.c400, flex:1}}>홍선수 #5</span>
          <button style={{fontSize:11, padding:'5px 10px', borderRadius:7, border:'1px solid '+C.p200, color:C.p500, background:'#fff', cursor:'pointer', fontWeight:600}}>알림</button>
        </div>
      </div>
      <div style={{height:8}} />
    </div>
  </div></div>);
}

/* ═══ MY ═══ */
function S_MyPage() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <NavBar large title="마이페이지" />
    <div className="dgs-scroll" style={{flex:1, padding:'0 20px', display:'flex', flexDirection:'column', gap:10}}>
      <div className="dgs-card" style={{display:'flex', alignItems:'center', gap:12}}>
        <div style={{width:54, height:54, borderRadius:27, background:C.c200}} />
        <div style={{flex:1}}>
          <div style={{fontSize:15, fontWeight:700}}>김주장</div>
          <div style={{fontSize:11, color:C.c500, marginTop:2}}>카카오로 로그인됨</div>
        </div>
        <span style={{color:C.c500, fontSize:18}}>›</span>
      </div>
      <div style={{background:'#fff', borderRadius:12, boxShadow:'0 1px 2px rgba(31,30,27,0.04)', overflow:'hidden'}}>
        {[['소속 팀','2개 ›'],['용병 프로필','설정 ›'],['알림 설정','›'],['계정 관리','›']].map(([l,r],i,a)=>(
          <div key={l} style={{padding:'13px 14px', display:'flex', justifyContent:'space-between', alignItems:'center', borderBottom:i<a.length-1?'1px solid '+C.c100:'none'}}>
            <span style={{fontSize:13}}>{l}</span><span style={{fontSize:12, color:C.c500}}>{r}</span>
          </div>
        ))}
      </div>
      <div style={{background:'#fff', borderRadius:12, boxShadow:'0 1px 2px rgba(31,30,27,0.04)'}}><button style={{width:'100%', padding:'13px', background:'none', border:'none', color:C.danger, fontSize:13, cursor:'pointer'}}>로그아웃</button></div>
      <div style={{height:8}} />
    </div>
    <TabBar active={4} />
  </div></div>);
}
function S_NotifCenter() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div className="dgs-nb" style={{borderBottom:'1px solid '+C.c100}}>
      <button className="dgs-nib" style={{fontSize:22, marginLeft:-4}}>‹</button>
      <span style={{fontSize:16, fontWeight:600}}>알림</span>
      <button className="dgs-btn-g" style={{padding:0, fontSize:12}}>모두 읽음</button>
    </div>
    <div className="dgs-scroll" style={{flex:1, padding:'14px 20px 12px'}}>
      <div style={{fontSize:10, fontWeight:600, color:C.c500, letterSpacing:1, textTransform:'uppercase'}}>수원 라이거스</div>
      <div style={{marginTop:8, display:'flex', flexDirection:'column', gap:8}}>
        <div style={{background:C.p50, borderRadius:12, padding:12, display:'flex', gap:8, borderLeft:'3px solid '+C.p500}}>
          <span>📅</span>
          <div style={{flex:1}}>
            <div style={{fontSize:13, fontWeight:600}}>새 경기 일정 등록</div>
            <div style={{fontSize:11, color:C.c500, marginTop:2}}>5/10 (토) 14:00 · 잠실 한강 야구장</div>
            <div style={{fontSize:10, color:C.c400, marginTop:4}}>2시간 전</div>
          </div>
        </div>
        <div style={{background:'#fff', borderRadius:12, padding:12, display:'flex', gap:8, boxShadow:'0 1px 2px rgba(31,30,27,0.04)'}}>
          <span>⏰</span>
          <div style={{flex:1}}>
            <div style={{fontSize:13}}>출석 투표 24시간 전</div>
            <div style={{fontSize:11, color:C.c500, marginTop:2}}>vs 청룡 BC · 응답해주세요</div>
            <div style={{fontSize:10, color:C.c400, marginTop:4}}>어제</div>
          </div>
        </div>
      </div>
      <div style={{fontSize:10, fontWeight:600, color:C.c500, letterSpacing:1, textTransform:'uppercase', marginTop:18}}>FC 청춘</div>
      <div style={{marginTop:8}}>
        <div style={{background:'#fff', borderRadius:12, padding:12, display:'flex', gap:8, boxShadow:'0 1px 2px rgba(31,30,27,0.04)'}}>
          <span>💰</span>
          <div style={{flex:1}}>
            <div style={{fontSize:13}}>5월 회비 미납 안내</div>
            <div style={{fontSize:11, color:C.c500, marginTop:2}}>25,000원 · 5/15 까지</div>
          </div>
        </div>
      </div>
    </div>
  </div></div>);
}

/* ═══ STATES ═══ */
function S_StateEmpty() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div style={{flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', textAlign:'center', padding:'0 32px'}}>
      <div style={{fontSize:58, color:C.c300, marginBottom:8}}>📅</div>
      <div style={{fontSize:15, fontWeight:700}}>예정된 경기가 없어요</div>
      <div style={{fontSize:12, color:C.c500, marginTop:6}}>주장에게 일정 등록을 요청해보세요</div>
      <button className="dgs-btn-s" style={{marginTop:18, width:'auto', padding:'10px 20px'}}>경기 등록</button>
    </div>
  </div></div>);
}
function S_StateLoading() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div style={{flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center'}}>
      <div style={{width:36, height:36, border:'4px solid '+C.p200, borderTop:'4px solid '+C.p500, borderRadius:'50%', animation:'dgs-spin 0.9s linear infinite'}} />
      <div style={{fontSize:12, color:C.c500, marginTop:10}}>불러오는 중...</div>
    </div>
  </div></div>);
}
function S_StateError() {
  return (<div className="dgs-device"><div className="dgs-screen">
    <StatusBar />
    <div style={{flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', textAlign:'center', padding:'0 32px'}}>
      <div style={{fontSize:48, color:C.warning, marginBottom:8}}>⚠</div>
      <div style={{fontSize:15, fontWeight:700}}>잠시 후 다시 시도해주세요</div>
      <div style={{fontSize:12, color:C.c500, marginTop:6}}>네트워크 연결을 확인해주세요</div>
      <button className="dgs-btn-s" style={{marginTop:18, width:'auto', padding:'10px 20px'}}>다시 시도</button>
    </div>
  </div></div>);
}

Object.assign(window, {
  C, StatusBar, TabBar, NavBar, Badge,
  S_Splash, S_Login, S_OnboardNickname, S_OnboardPosition, S_OnboardChoose,
  S_HomeEmpty, S_HomeDashboard,
  S_TeamList, S_TeamCreate, S_TeamJoin, S_TeamDetail, S_MemberDialog,
  S_MatchList, S_MatchCreate, S_MatchDetail, S_AttVote, S_AttSummary,
  S_MyPage, S_NotifCenter,
  S_StateEmpty, S_StateLoading, S_StateError,
});
