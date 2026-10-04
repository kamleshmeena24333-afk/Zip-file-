package com.example.engine

object TemplateProvider {

    data class TemplateInfo(
        val id: String,
        val title: String,
        val description: String,
        val category: String,
        val iconEmoji: String,
        val themeColorHex: String,
        val defaultPackageName: String,
        val orientation: String = "portrait",
        val files: Map<String, String>
    )

    fun getAllTemplates(): List<TemplateInfo> = listOf(
        getFlappyDroidTemplate(),
        getNotesTaskTemplate(),
        getCalculatorTemplate(),
        getStoreCatalogTemplate(),
        getBeatForgeTemplate()
    )

    private fun getFlappyDroidTemplate(): TemplateInfo {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Flappy Droid</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div id="game-container">
        <canvas id="gameCanvas"></canvas>
        <div id="ui-overlay">
            <div id="score-display">0</div>
            <div id="start-screen" class="screen">
                <div class="logo">🤖 FLAPPY DROID</div>
                <p>Tap or Click to Fly!</p>
                <button id="start-btn" class="glow-btn">START GAME</button>
            </div>
            <div id="game-over-screen" class="screen hidden">
                <h2>GAME OVER</h2>
                <div class="score-card">
                    <p>Score: <span id="final-score">0</span></p>
                    <p>Best: <span id="best-score">0</span></p>
                </div>
                <button id="restart-btn" class="glow-btn">PLAY AGAIN</button>
            </div>
        </div>
    </div>
    <script src="game.js"></script>
</body>
</html>
        """.trimIndent()

        val css = """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    user-select: none;
    -webkit-user-select: none;
}
body {
    background: #0f172a;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    overflow: hidden;
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
}
#game-container {
    position: relative;
    width: 100%;
    max-width: 480px;
    height: 100vh;
    overflow: hidden;
    background: linear-gradient(to bottom, #1e3a8a, #3b82f6 60%, #10b981 100%);
}
#gameCanvas {
    width: 100%;
    height: 100%;
    display: block;
}
#ui-overlay {
    position: absolute;
    inset: 0;
    pointer-events: none;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    padding: 24px;
}
#score-display {
    font-size: 52px;
    font-weight: 900;
    color: #ffffff;
    text-shadow: 0 4px 10px rgba(0,0,0,0.5);
    margin-top: 20px;
    z-index: 10;
}
.screen {
    pointer-events: auto;
    position: absolute;
    inset: 0;
    background: rgba(15, 23, 42, 0.85);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
    padding: 20px;
    backdrop-filter: blur(8px);
    transition: opacity 0.3s ease;
}
.hidden {
    display: none !important;
}
.logo {
    font-size: 32px;
    font-weight: 900;
    color: #38bdf8;
    margin-bottom: 12px;
    letter-spacing: 2px;
    text-shadow: 0 0 20px rgba(56, 189, 248, 0.6);
}
p {
    color: #94a3b8;
    font-size: 16px;
    margin-bottom: 24px;
}
.glow-btn {
    background: linear-gradient(135deg, #10b981, #059669);
    color: white;
    font-size: 18px;
    font-weight: bold;
    padding: 14px 36px;
    border: none;
    border-radius: 30px;
    cursor: pointer;
    box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
    transition: transform 0.1s, box-shadow 0.1s;
}
.glow-btn:active {
    transform: scale(0.96);
}
.score-card {
    background: rgba(255,255,255,0.08);
    border: 1px solid rgba(255,255,255,0.15);
    border-radius: 16px;
    padding: 20px 40px;
    margin-bottom: 24px;
}
.score-card p {
    font-size: 20px;
    color: #ffffff;
    margin: 8px 0;
}
.score-card span {
    color: #facc15;
    font-weight: bold;
}
        """.trimIndent()

        val js = """
const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const scoreDisplay = document.getElementById('score-display');
const startScreen = document.getElementById('start-screen');
const gameOverScreen = document.getElementById('game-over-screen');
const finalScoreEl = document.getElementById('final-score');
const bestScoreEl = document.getElementById('best-score');
const startBtn = document.getElementById('start-btn');
const restartBtn = document.getElementById('restart-btn');

function resizeCanvas() {
    canvas.width = canvas.clientWidth || 360;
    canvas.height = canvas.clientHeight || 640;
}
window.addEventListener('resize', resizeCanvas);
resizeCanvas();

// Web Audio Sound FX Synthesizer
const AudioContext = window.AudioContext || window.webkitAudioContext;
let audioCtx = null;
function playSound(type) {
    try {
        if (!audioCtx) audioCtx = new AudioContext();
        if (audioCtx.state === 'suspended') audioCtx.resume();
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        const now = audioCtx.currentTime;

        if (type === 'jump') {
            osc.frequency.setValueAtTime(320, now);
            osc.frequency.exponentialRampToValueAtTime(640, now + 0.12);
            gain.gain.setValueAtTime(0.2, now);
            gain.gain.exponentialRampToValueAtTime(0.01, now + 0.12);
            osc.start(now);
            osc.stop(now + 0.12);
        } else if (type === 'score') {
            osc.frequency.setValueAtTime(587.33, now);
            osc.frequency.setValueAtTime(880, now + 0.08);
            gain.gain.setValueAtTime(0.25, now);
            gain.gain.exponentialRampToValueAtTime(0.01, now + 0.2);
            osc.start(now);
            osc.stop(now + 0.2);
        } else if (type === 'hit') {
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(220, now);
            osc.frequency.exponentialRampToValueAtTime(60, now + 0.25);
            gain.gain.setValueAtTime(0.3, now);
            gain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);
            osc.start(now);
            osc.stop(now + 0.25);
        }
    } catch(e) {
        console.warn("Audio not permitted yet:", e);
    }
}

// Game State
let gameState = 'START';
let score = 0;
let bestScore = parseInt(localStorage.getItem('flappy_droid_best') || '0');
let frames = 0;

const droid = {
    x: 60,
    y: 200,
    radius: 18,
    gravity: 0.38,
    velocity: 0,
    jumpStrength: -6.8,
    draw() {
        ctx.save();
        ctx.translate(this.x, this.y);
        ctx.rotate(Math.min(Math.PI / 4, Math.max(-Math.PI / 4, this.velocity * 0.06)));
        
        // Android body
        ctx.fillStyle = '#34d399';
        ctx.beginPath();
        ctx.arc(0, 0, this.radius, 0, Math.PI * 2);
        ctx.fill();

        // Eyes
        ctx.fillStyle = '#0f172a';
        ctx.beginPath();
        ctx.arc(6, -4, 3, 0, Math.PI * 2);
        ctx.fill();

        // Antenna
        ctx.strokeStyle = '#34d399';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(2, -14);
        ctx.lineTo(8, -22);
        ctx.stroke();

        ctx.restore();
    },
    update() {
        this.velocity += this.gravity;
        this.y += this.velocity;
        if (this.y + this.radius >= canvas.height - 40) {
            this.y = canvas.height - 40 - this.radius;
            gameOver();
        }
        if (this.y - this.radius <= 0) {
            this.y = this.radius;
            this.velocity = 0;
        }
    },
    jump() {
        this.velocity = this.jumpStrength;
        playSound('jump');
    }
};

const pipes = [];
const pipeWidth = 56;
const pipeGap = 160;
const pipeSpeed = 2.4;

function spawnPipe() {
    const minHeight = 60;
    const maxHeight = canvas.height - pipeGap - minHeight - 60;
    const topHeight = Math.floor(Math.random() * (maxHeight - minHeight + 1)) + minHeight;
    pipes.push({
        x: canvas.width,
        top: topHeight,
        bottom: canvas.height - topHeight - pipeGap - 40,
        passed: false
    });
}

function resetGame() {
    droid.y = canvas.height / 2;
    droid.velocity = 0;
    pipes.length = 0;
    score = 0;
    frames = 0;
    scoreDisplay.innerText = '0';
    gameState = 'PLAYING';
    startScreen.classList.add('hidden');
    gameOverScreen.classList.add('hidden');
}

function gameOver() {
    if (gameState === 'GAMEOVER') return;
    gameState = 'GAMEOVER';
    playSound('hit');
    if (score > bestScore) {
        bestScore = score;
        localStorage.setItem('flappy_droid_best', bestScore.toString());
    }
    finalScoreEl.innerText = score;
    bestScoreEl.innerText = bestScore;
    gameOverScreen.classList.remove('hidden');
}

function loop() {
    frames++;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Ground
    ctx.fillStyle = '#1e293b';
    ctx.fillRect(0, canvas.height - 40, canvas.width, 40);
    ctx.fillStyle = '#10b981';
    ctx.fillRect(0, canvas.height - 40, canvas.width, 6);

    if (gameState === 'PLAYING') {
        if (frames % 100 === 0) {
            spawnPipe();
        }

        for (let i = pipes.length - 1; i >= 0; i--) {
            const p = pipes[i];
            p.x -= pipeSpeed;

            // Draw Pipes
            ctx.fillStyle = '#059669';
            ctx.strokeStyle = '#047857';
            ctx.lineWidth = 3;
            // Top pipe
            ctx.fillRect(p.x, 0, pipeWidth, p.top);
            ctx.strokeRect(p.x, 0, pipeWidth, p.top);
            // Bottom pipe
            const bottomY = canvas.height - 40 - p.bottom;
            ctx.fillRect(p.x, bottomY, pipeWidth, p.bottom);
            ctx.strokeRect(p.x, bottomY, pipeWidth, p.bottom);

            // Collision Check
            if (
                droid.x + droid.radius > p.x &&
                droid.x - droid.radius < p.x + pipeWidth
            ) {
                if (
                    droid.y - droid.radius < p.top ||
                    droid.y + droid.radius > bottomY
                ) {
                    gameOver();
                }
            }

            // Score Check
            if (!p.passed && p.x + pipeWidth < droid.x) {
                p.passed = true;
                score++;
                scoreDisplay.innerText = score;
                playSound('score');
            }

            // Remove off-screen
            if (p.x + pipeWidth < 0) {
                pipes.splice(i, 1);
            }
        }

        droid.update();
    }

    droid.draw();
    requestAnimationFrame(loop);
}

startBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    resetGame();
});

restartBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    resetGame();
});

function handleAction() {
    if (gameState === 'PLAYING') {
        droid.jump();
    }
}

window.addEventListener('keydown', (e) => {
    if (e.code === 'Space') {
        handleAction();
    }
});

canvas.addEventListener('touchstart', (e) => {
    e.preventDefault();
    handleAction();
}, { passive: false });

canvas.addEventListener('mousedown', () => {
    handleAction();
});

loop();
console.log("Flappy Droid initialized successfully!");
        """.trimIndent()

        return TemplateInfo(
            id = "flappy_droid",
            title = "Flappy Droid Game",
            description = "Fast 60FPS arcade game with touch controls, physics, sound synth and high scores.",
            category = "Game",
            iconEmoji = "🎮",
            themeColorHex = "#10B981",
            defaultPackageName = "com.apkmaker.flappydroid",
            orientation = "portrait",
            files = mapOf(
                "index.html" to html,
                "style.css" to css,
                "game.js" to js
            )
        )
    }

    private fun getNotesTaskTemplate(): TemplateInfo {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>TaskFlow Studio</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header class="app-bar">
        <h1>TaskFlow</h1>
        <div class="stats"><span id="pending-count">0</span> pending</div>
    </header>

    <main class="container">
        <div class="input-card">
            <input type="text" id="task-input" placeholder="Add a new task or note..." />
            <select id="category-select">
                <option value="Work">💼 Work</option>
                <option value="Personal">🏠 Personal</option>
                <option value="Urgent">⚡ Urgent</option>
            </select>
            <button id="add-btn">+</button>
        </div>

        <div class="filters">
            <button class="filter-chip active" data-filter="all">All</button>
            <button class="filter-chip" data-filter="pending">Pending</button>
            <button class="filter-chip" data-filter="completed">Done</button>
        </div>

        <ul id="task-list" class="task-list"></ul>
    </main>

    <script src="app.js"></script>
</body>
</html>
        """.trimIndent()

        val css = """
* { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, sans-serif; }
body { background: #0f172a; color: #f8fafc; min-height: 100vh; padding-bottom: 40px; }
.app-bar { background: #1e293b; padding: 20px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #334155; }
.app-bar h1 { font-size: 22px; font-weight: 800; background: linear-gradient(135deg, #60a5fa, #a855f7); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.stats { font-size: 13px; color: #94a3b8; background: #334155; padding: 4px 12px; border-radius: 20px; }
.container { max-width: 540px; margin: 0 auto; padding: 20px 16px; }
.input-card { display: flex; gap: 8px; background: #1e293b; padding: 10px; border-radius: 16px; border: 1px solid #334155; margin-bottom: 20px; }
.input-card input { flex: 1; background: transparent; border: none; color: white; padding: 8px 12px; font-size: 15px; outline: none; }
.input-card select { background: #0f172a; color: #94a3b8; border: 1px solid #334155; border-radius: 8px; padding: 6px; font-size: 13px; outline: none; }
.input-card button { width: 44px; height: 44px; border-radius: 12px; background: #3b82f6; color: white; border: none; font-size: 24px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: bold; }
.filters { display: flex; gap: 8px; margin-bottom: 16px; }
.filter-chip { background: #1e293b; color: #94a3b8; border: 1px solid #334155; border-radius: 20px; padding: 6px 16px; font-size: 13px; cursor: pointer; }
.filter-chip.active { background: #3b82f6; color: white; border-color: #3b82f6; }
.task-list { list-style: none; display: flex; flex-direction: column; gap: 10px; }
.task-item { background: #1e293b; border: 1px solid #334155; border-radius: 14px; padding: 14px; display: flex; align-items: center; gap: 12px; transition: transform 0.15s; }
.task-item.done { opacity: 0.55; text-decoration: line-through; }
.task-checkbox { width: 22px; height: 22px; cursor: pointer; accent-color: #3b82f6; }
.task-title { flex: 1; font-size: 15px; }
.badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: bold; }
.badge.Work { background: #1e3a8a; color: #60a5fa; }
.badge.Personal { background: #064e3b; color: #34d399; }
.badge.Urgent { background: #7f1d1d; color: #f87171; }
.delete-btn { background: none; border: none; color: #64748b; font-size: 18px; cursor: pointer; padding: 4px; }
.delete-btn:hover { color: #ef4444; }
        """.trimIndent()

        val js = """
let tasks = JSON.parse(localStorage.getItem('taskflow_items') || '[]');
let currentFilter = 'all';

const taskInput = document.getElementById('task-input');
const categorySelect = document.getElementById('category-select');
const addBtn = document.getElementById('add-btn');
const taskList = document.getElementById('task-list');
const pendingCount = document.getElementById('pending-count');
const filterChips = document.querySelectorAll('.filter-chip');

function save() {
    localStorage.setItem('taskflow_items', JSON.stringify(tasks));
    render();
}

function render() {
    const pending = tasks.filter(t => !t.done).length;
    pendingCount.innerText = pending;

    const filtered = tasks.filter(t => {
        if (currentFilter === 'pending') return !t.done;
        if (currentFilter === 'completed') return t.done;
        return true;
    });

    taskList.innerHTML = '';
    if (filtered.length === 0) {
        taskList.innerHTML = '<li style="text-align:center; color:#64748b; padding:30px;">No tasks here. Enjoy your day! ✨</li>';
        return;
    }

    filtered.forEach(t => {
        const li = document.createElement('li');
        li.className = 'task-item' + (t.done ? ' done' : '');
        li.innerHTML = '<input type="checkbox" class="task-checkbox" ' + (t.done ? 'checked' : '') + '>' +
            '<span class="task-title">' + escapeHtml(t.title) + '</span>' +
            '<span class="badge ' + t.category + '">' + t.category + '</span>' +
            '<button class="delete-btn">✕</button>';

        li.querySelector('.task-checkbox').addEventListener('change', () => {
            t.done = !t.done;
            save();
        });

        li.querySelector('.delete-btn').addEventListener('click', () => {
            tasks = tasks.filter(item => item.id !== t.id);
            save();
        });

        taskList.appendChild(li);
    });
}

function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function addTask() {
    const title = taskInput.value.trim();
    if (!title) return;
    tasks.unshift({
        id: Date.now(),
        title: title,
        category: categorySelect.value,
        done: false
    });
    taskInput.value = '';
    save();
}

addBtn.addEventListener('click', addTask);
taskInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') addTask(); });

filterChips.forEach(chip => {
    chip.addEventListener('click', () => {
        filterChips.forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        currentFilter = chip.dataset.filter;
        render();
    });
});

if (tasks.length === 0) {
    tasks = [
        { id: 1, title: 'Welcome to TaskFlow! 🎉', category: 'Personal', done: false },
        { id: 2, title: 'Export this app as a phone APK', category: 'Work', done: false },
        { id: 3, title: 'Check live preview on mobile', category: 'Urgent', done: true }
    ];
}
render();
console.log("TaskFlow loaded with", tasks.length, "items.");
        """.trimIndent()

        return TemplateInfo(
            id = "taskflow_notes",
            title = "TaskFlow - Notes & Tasks",
            description = "Material 3 todo organizer with categories, task filters, and offline storage.",
            category = "Notes",
            iconEmoji = "📝",
            themeColorHex = "#3B82F6",
            defaultPackageName = "com.apkmaker.taskflow",
            orientation = "portrait",
            files = mapOf(
                "index.html" to html,
                "style.css" to css,
                "app.js" to js
            )
        )
    }

    private fun getCalculatorTemplate(): TemplateInfo {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>NeoCalc Pro</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="calc-container">
        <div class="display">
            <div id="history"></div>
            <div id="current">0</div>
        </div>
        <div class="keypad">
            <button class="btn fn" data-action="clear">AC</button>
            <button class="btn fn" data-action="delete">⌫</button>
            <button class="btn fn" data-action="percent">%</button>
            <button class="btn op" data-action="op" data-val="/">÷</button>

            <button class="btn num" data-val="7">7</button>
            <button class="btn num" data-val="8">8</button>
            <button class="btn num" data-val="9">9</button>
            <button class="btn op" data-action="op" data-val="*">×</button>

            <button class="btn num" data-val="4">4</button>
            <button class="btn num" data-val="5">5</button>
            <button class="btn num" data-val="6">6</button>
            <button class="btn op" data-action="op" data-val="-">−</button>

            <button class="btn num" data-val="1">1</button>
            <button class="btn num" data-val="2">2</button>
            <button class="btn num" data-val="3">3</button>
            <button class="btn op" data-action="op" data-val="+">+</button>

            <button class="btn num" data-val="00">00</button>
            <button class="btn num" data-val="0">0</button>
            <button class="btn num" data-val=".">.</button>
            <button class="btn equals" data-action="equals">=</button>
        </div>
    </div>
    <script src="calc.js"></script>
</body>
</html>
        """.trimIndent()

        val css = """
* { margin:0; padding:0; box-sizing:border-box; font-family:'Roboto', system-ui, sans-serif; }
body { background:#0a0f1d; min-height:100vh; display:flex; justify-content:center; align-items:center; }
.calc-container { width:100%; max-width:380px; height:100vh; max-height:720px; background:#111827; border-radius:24px; padding:24px; display:flex; flex-direction:column; justify-content:flex-end; box-shadow:0 12px 36px rgba(0,0,0,0.5); }
.display { text-align:right; margin-bottom:24px; padding:12px; }
#history { color:#6b7280; font-size:18px; min-height:24px; }
#current { color:#f9fafb; font-size:48px; font-weight:700; word-break:break-all; }
.keypad { display:grid; grid-template-columns:repeat(4, 1fr); gap:12px; }
.btn { height:64px; border:none; border-radius:18px; font-size:22px; font-weight:600; cursor:pointer; background:#1f2937; color:#f3f4f6; transition:transform 0.1s, opacity 0.1s; }
.btn:active { transform:scale(0.92); opacity:0.8; }
.btn.fn { background:#374151; color:#38bdf8; }
.btn.op { background:#4f46e5; color:#ffffff; }
.btn.equals { background:linear-gradient(135deg, #10b981, #059669); color:#ffffff; }
        """.trimIndent()

        val js = """
let current = '0';
let history = '';
let pendingOp = null;
let lastValue = 0;

const currentEl = document.getElementById('current');
const historyEl = document.getElementById('history');

function updateDisplay() {
    currentEl.innerText = current;
    historyEl.innerText = history;
}

document.querySelectorAll('.btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const val = btn.dataset.val;
        const action = btn.dataset.action;

        if (val && !action) {
            if (current === '0' && val !== '.') current = val;
            else current += val;
        } else if (action === 'clear') {
            current = '0';
            history = '';
            pendingOp = null;
        } else if (action === 'delete') {
            current = current.length > 1 ? current.slice(0, -1) : '0';
        } else if (action === 'percent') {
            current = (parseFloat(current) / 100).toString();
        } else if (action === 'op') {
            lastValue = parseFloat(current);
            pendingOp = val;
            history = current + ' ' + btn.innerText;
            current = '0';
        } else if (action === 'equals') {
            if (pendingOp) {
                const second = parseFloat(current);
                let result = 0;
                if (pendingOp === '+') result = lastValue + second;
                if (pendingOp === '-') result = lastValue - second;
                if (pendingOp === '*') result = lastValue * second;
                if (pendingOp === '/') result = second !== 0 ? lastValue / second : 'Error';
                history = history + ' ' + current + ' =';
                current = result.toString();
                pendingOp = null;
            }
        }
        updateDisplay();
    });
});
console.log("NeoCalc initialized");
        """.trimIndent()

        return TemplateInfo(
            id = "neocalc_pro",
            title = "NeoCalc Pro Calculator",
            description = "Clean modern calculator with instant arithmetic, responsive keypad, and history tape.",
            category = "Utility",
            iconEmoji = "🧮",
            themeColorHex = "#4F46E5",
            defaultPackageName = "com.apkmaker.neocalc",
            orientation = "portrait",
            files = mapOf(
                "index.html" to html,
                "style.css" to css,
                "calc.js" to js
            )
        )
    }

    private fun getStoreCatalogTemplate(): TemplateInfo {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>SwiftStore</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header class="navbar">
        <h2>🛍️ SwiftStore</h2>
        <div class="cart-badge" id="cartBtn">🛒 <span id="cartCount">0</span></div>
    </header>

    <div class="banner">
        <h3>🔥 Mega Tech Fest</h3>
        <p>Flat 40% OFF on all Gadgets!</p>
    </div>

    <div class="catalog" id="catalog"></div>

    <div id="cartModal" class="modal hidden">
        <div class="modal-content">
            <h3>Your Shopping Cart</h3>
            <div id="cartItems"></div>
            <div class="total-row">Total: <b id="cartTotal">₹0</b></div>
            <button id="checkoutBtn" class="order-btn">Checkout on WhatsApp 💬</button>
            <button id="closeCartBtn" class="close-btn">Close</button>
        </div>
    </div>
    <script src="store.js"></script>
</body>
</html>
        """.trimIndent()

        val css = """
* { margin:0; padding:0; box-sizing:border-box; font-family:system-ui, sans-serif; }
body { background:#0f172a; color:#f8fafc; padding-bottom:30px; }
.navbar { background:#1e293b; padding:16px 20px; display:flex; justify-content:space-between; align-items:center; position:sticky; top:0; z-index:100; border-bottom:1px solid #334155; }
.cart-badge { background:#3b82f6; padding:8px 16px; border-radius:20px; font-weight:bold; cursor:pointer; }
.banner { background:linear-gradient(135deg, #ec4899, #8b5cf6); padding:24px; text-align:center; margin:16px; border-radius:16px; }
.catalog { display:grid; grid-template-columns:repeat(auto-fill, minmax(160px, 1fr)); gap:14px; padding:0 16px; }
.card { background:#1e293b; border-radius:14px; padding:14px; border:1px solid #334155; display:flex; flex-direction:column; justify-content:space-between; }
.card-icon { font-size:42px; text-align:center; margin:12px 0; }
.card h4 { font-size:15px; margin-bottom:4px; }
.price { color:#34d399; font-weight:bold; font-size:16px; margin-bottom:12px; }
.buy-btn { background:#3b82f6; color:white; border:none; padding:8px; border-radius:8px; font-weight:bold; cursor:pointer; }
.modal { position:fixed; inset:0; background:rgba(0,0,0,0.8); display:flex; justify-content:center; align-items:center; z-index:200; }
.modal.hidden { display:none; }
.modal-content { background:#1e293b; width:90%; max-width:400px; padding:24px; border-radius:20px; border:1px solid #334155; }
.total-row { font-size:18px; margin:16px 0; text-align:right; }
.order-btn { width:100%; background:#22c55e; color:white; border:none; padding:12px; border-radius:12px; font-size:16px; font-weight:bold; cursor:pointer; margin-bottom:8px; }
.close-btn { width:100%; background:#334155; color:white; border:none; padding:10px; border-radius:12px; cursor:pointer; }
        """.trimIndent()

        val js = """
const products = [
    { id:1, name:'Wireless Earbuds Pro', price:1999, emoji:'🎧' },
    { id:2, name:'Smart Fitness Band 7', price:2499, emoji:'⌚' },
    { id:3, name:'Ultra Gaming Mouse', price:1299, emoji:'🖱️' },
    { id:4, name:'Mechanical Keyboard', price:3499, emoji:'⌨️' },
    { id:5, name:'Fast GaN Charger 65W', price:899, emoji:'🔌' },
    { id:6, name:'Bluetooth RGB Speaker', price:1799, emoji:'🔊' }
];

let cart = [];

function renderCatalog() {
    const catalog = document.getElementById('catalog');
    catalog.innerHTML = products.map(p =>
        '<div class="card">' +
            '<div class="card-icon">' + p.emoji + '</div>' +
            '<h4>' + p.name + '</h4>' +
            '<div class="price">₹' + p.price + '</div>' +
            '<button class="buy-btn" onclick="addToCart(' + p.id + ')">Add to Cart</button>' +
        '</div>'
    ).join('');
}

window.addToCart = function(id) {
    const prod = products.find(p => p.id === id);
    if (prod) {
        cart.push(prod);
        document.getElementById('cartCount').innerText = cart.length;
        alert("Added " + prod.name + " to cart!");
    }
};

document.getElementById('cartBtn').onclick = () => {
    document.getElementById('cartModal').classList.remove('hidden');
    const itemsEl = document.getElementById('cartItems');
    const total = cart.reduce((acc, cur) => acc + cur.price, 0);
    document.getElementById('cartTotal').innerText = '₹' + total;
    if (cart.length === 0) {
        itemsEl.innerHTML = '<p style="color:#94a3b8; margin:16px 0;">Cart is empty!</p>';
    } else {
        itemsEl.innerHTML = cart.map(i => '<div style="display:flex; justify-content:space-between; margin:8px 0;"><span>' + i.emoji + ' ' + i.name + '</span><b>₹' + i.price + '</b></div>').join('');
    }
};

document.getElementById('closeCartBtn').onclick = () => {
    document.getElementById('cartModal').classList.add('hidden');
};

document.getElementById('checkoutBtn').onclick = () => {
    if (cart.length === 0) return alert("Your cart is empty!");
    const total = cart.reduce((acc, cur) => acc + cur.price, 0);
    alert("Ordering " + cart.length + " items for ₹" + total + " via WhatsApp!");
};

renderCatalog();
console.log("SwiftStore initialized");
        """.trimIndent()

        return TemplateInfo(
            id = "swift_store",
            title = "SwiftStore - E-Commerce",
            description = "Product catalog, shopping cart with total calculation, and WhatsApp direct checkout.",
            category = "Store",
            iconEmoji = "🛍️",
            themeColorHex = "#EC4899",
            defaultPackageName = "com.apkmaker.swiftstore",
            orientation = "portrait",
            files = mapOf(
                "index.html" to html,
                "style.css" to css,
                "store.js" to js
            )
        )
    }

    private fun getBeatForgeTemplate(): TemplateInfo {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>BeatForge Studio</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header>
        <h1>🎵 BeatForge</h1>
        <p>Interactive Drum Pads & Synth Engine</p>
    </header>

    <div class="pad-grid">
        <button class="pad red" data-freq="80" data-name="Bass Kick">Kick</button>
        <button class="pad orange" data-freq="250" data-name="Snare">Snare</button>
        <button class="pad yellow" data-freq="800" data-name="Hi-Hat">Hi-Hat</button>
        <button class="pad green" data-freq="330" data-name="Tom 1">Tom 1</button>
        <button class="pad cyan" data-freq="440" data-name="Tom 2">Tom 2</button>
        <button class="pad blue" data-freq="550" data-name="Clap">Clap</button>
        <button class="pad purple" data-freq="660" data-name="Synth Lead">Synth</button>
        <button class="pad pink" data-freq="130" data-name="Sub Drop">Sub</button>
        <button class="pad white" data-freq="990" data-name="Laser">Laser</button>
    </div>

    <div class="visualizer-bar" id="visBar"></div>
    <script src="synth.js"></script>
</body>
</html>
        """.trimIndent()

        val css = """
* { margin:0; padding:0; box-sizing:border-box; font-family:sans-serif; user-select:none; }
body { background:#0a0a14; color:#fff; min-height:100vh; display:flex; flex-direction:column; align-items:center; justify-content:center; padding:20px; }
header { text-align:center; margin-bottom:24px; }
header h1 { font-size:32px; background:linear-gradient(45deg, #f43f5e, #8b5cf6); -webkit-background-clip:text; -webkit-text-fill-color:transparent; }
header p { color:#94a3b8; font-size:14px; }
.pad-grid { display:grid; grid-template-columns:repeat(3, 1fr); gap:16px; width:100%; max-width:360px; }
.pad { aspect-ratio:1; border:none; border-radius:20px; font-size:18px; font-weight:bold; color:white; cursor:pointer; box-shadow:0 8px 24px rgba(0,0,0,0.4); transition:transform 0.08s, filter 0.08s; }
.pad:active { transform:scale(0.92); filter:brightness(1.4); }
.pad.red { background:linear-gradient(135deg, #ef4444, #b91c1c); }
.pad.orange { background:linear-gradient(135deg, #f97316, #c2410c); }
.pad.yellow { background:linear-gradient(135deg, #eab308, #a16207); }
.pad.green { background:linear-gradient(135deg, #22c55e, #15803d); }
.pad.cyan { background:linear-gradient(135deg, #06b6d4, #0e7490); }
.pad.blue { background:linear-gradient(135deg, #3b82f6, #1d4ed8); }
.pad.purple { background:linear-gradient(135deg, #a855f7, #7e22ce); }
.pad.pink { background:linear-gradient(135deg, #ec4899, #be185d); }
.pad.white { background:linear-gradient(135deg, #64748b, #334155); }
.visualizer-bar { height:6px; width:100%; max-width:360px; background:#1e293b; border-radius:3px; margin-top:24px; overflow:hidden; }
        """.trimIndent()

        val js = """
const AudioContext = window.AudioContext || window.webkitAudioContext;
let ctx = null;

function playTone(freq) {
    if (!ctx) ctx = new AudioContext();
    if (ctx.state === 'suspended') ctx.resume();

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);

    const now = ctx.currentTime;
    osc.frequency.setValueAtTime(freq, now);
    osc.frequency.exponentialRampToValueAtTime(30, now + 0.35);

    gain.gain.setValueAtTime(0.4, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35);

    osc.start(now);
    osc.stop(now + 0.35);
}

document.querySelectorAll('.pad').forEach(pad => {
    pad.addEventListener('pointerdown', (e) => {
        e.preventDefault();
        const freq = parseFloat(pad.dataset.freq);
        playTone(freq);
    });
});
console.log("BeatForge synthesizer ready!");
        """.trimIndent()

        return TemplateInfo(
            id = "beat_forge",
            title = "BeatForge - Audio Drum Machine",
            description = "Interactive 9-pad drum machine with synthesized analog sounds using Web Audio API.",
            category = "Audio",
            iconEmoji = "🥁",
            themeColorHex = "#F43F5E",
            defaultPackageName = "com.apkmaker.beatforge",
            orientation = "portrait",
            files = mapOf(
                "index.html" to html,
                "style.css" to css,
                "synth.js" to js
            )
        )
    }
}
