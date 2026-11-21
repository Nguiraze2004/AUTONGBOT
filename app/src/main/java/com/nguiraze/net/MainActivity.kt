package com.nguiraze.net

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val bgTop = Color.rgb(5, 16, 42)
    private val bgBot = Color.rgb(4, 24, 68)
    private val cardBlue = Color.rgb(7, 34, 88)
    private val cardBorder = Color.rgb(20, 75, 165)
    private val cyan = Color.rgb(0, 215, 255)
    private val green = Color.rgb(0, 220, 125)
    private val white = Color.WHITE
    private val lightBlue = Color.rgb(160, 195, 240)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bgTop
        window.navigationBarColor = bgBot

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgTop, bgBot)
            )
        }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
        }

        // 1. Cabeçalho (Ícone + Título bicolor + Badge Online)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val avatar = txt("🤖", 20f, white).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(12, 45, 105))
                    setStroke(dp(1), cyan)
                }
            }
            addView(avatar, LinearLayout.LayoutParams(dp(44), dp(44)))

            val titleCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
                val titulo = SpannableString("NGUIRAZE NET").apply {
                    setSpan(ForegroundColorSpan(cyan), 9, 12, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                addView(TextView(this@MainActivity).apply {
                    text = titulo; textSize = 16f; setTextColor(white); setTypeface(null, Typeface.BOLD)
                })
                addView(txt("Versão instalada: 1.86 (86)", 11f, lightBlue))
            }
            addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

            val onlineBadge = txt("● Online", 11f, green, true).apply {
                setPadding(dp(12), dp(5), dp(12), dp(5))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(10, 52, 58))
                    cornerRadius = dp(20).toFloat()
                    setStroke(dp(1), Color.rgb(18, 110, 95))
                }
            }
            addView(onlineBadge)
        }
        body.addView(header)

        // Tag "icone de megas"
        body.addView(txt("  icone de megas", 11f, lightBlue).apply {
            setPadding(dp(12), dp(5), dp(14), dp(5))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10); bottomMargin = dp(14) }
            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 42, 102))
                cornerRadius = dp(20).toFloat()
            }
        })

        // 2. Cartão Central (Anel Ciano + Meta do Dia)
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(18), dp(14), dp(18))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) }
            background = GradientDrawable().apply {
                setColor(cardBlue); cornerRadius = dp(18).toFloat(); setStroke(dp(1), cardBorder)
            }

            addView(CyanRingView(this@MainActivity), LinearLayout.LayoutParams(dp(145), dp(145)))

            val metaCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
                addView(txt("Meta do dia", 12f, lightBlue))
                addView(txt("1000 GB", 18f, white, true).apply { setPadding(0, 0, 0, dp(10)) })
                addView(txt("Saldo vendido hoje", 12f, lightBlue))
                addView(txt("0 MT", 17f, white, true))
            }
            addView(metaCol, LinearLayout.LayoutParams(0, -2, 1f))
        }
        body.addView(mainCard)

        // 3. Linha dos 4 Botões Circulares
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) }
        }
        listOf("➤" to "Enviar megas", "☰" to "Fila", "▤" to "Histórico", "◉" to "Meta").forEach { (ico, nome) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val circle = txt(ico, 18f, white, true).apply {
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.rgb(14, 68, 175))
                        setStroke(dp(1), Color.rgb(45, 135, 255))
                    }
                }
                addView(circle, LinearLayout.LayoutParams(dp(54), dp(54)))
                addView(txt(nome, 11f, white, true).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0)
                })
            }
            actionsRow.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        body.addView(actionsRow)

        // 4. Título "Últimos pedidos" + Lista
        body.addView(txt("Últimos pedidos", 16f, white, true).apply { setPadding(0, 0, 0, dp(10)) })

        listOf(
            Triple("847337160", "Megas · 23:24", "10240 MB"),
            Triple("853767234", "Megas · 23:17", "10240 MB"),
            Triple("850387303", "Megas · 23:09", "10240 MB")
        ).forEach { (num, hora, mb) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
                background = GradientDrawable().apply {
                    setColor(Color.rgb(6, 32, 84)); cornerRadius = dp(14).toFloat(); setStroke(dp(1), cardBorder)
                }

                val check = txt("✓", 15f, green, true).apply {
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(12, 65, 75)) }
                }
                addView(check, LinearLayout.LayoutParams(dp(38), dp(38)))

                val mid = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0)
                    addView(txt(num, 15f, white, true))
                    addView(txt(hora, 12f, lightBlue))
                }
                addView(mid, LinearLayout.LayoutParams(0, -2, 1f))

                val right = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.END
                    addView(txt(mb, 14f, cyan, true))
                    addView(txt("Concluído", 10f, green, true).apply {
                        setPadding(dp(8), dp(2), dp(8), dp(2))
                        background = GradientDrawable().apply { setColor(Color.rgb(12, 65, 75)); cornerRadius = dp(10).toFloat() }
                    })
                }
                addView(right)
            }
            body.addView(row)
        }

        root.addView(ScrollView(this).apply { addView(body) })
        setContentView(root)
    }

    // Anel circular brilhante "1980,00 GB hoje"
    private inner class CyanRingView(ctx: Context) : View(ctx) {
        private val pBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(9).toFloat(); color = Color.rgb(14, 55, 130)
        }
        private val pFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(9).toFloat(); strokeCap = Paint.Cap.ROUND; color = cyan
        }
        private val pBig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white; textSize = dp(20).toFloat(); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        private val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightBlue; textSize = dp(11).toFloat(); textAlign = Paint.Align.CENTER
        }

        override fun onDraw(c: Canvas) {
            val s = width.coerceAtMost(height).toFloat()
            val pad = dp(12).toFloat()
            val rect = RectF(pad, pad, s - pad, s - pad)
            c.drawArc(rect, 0f, 360f, false, pBg)
            c.drawArc(rect, -90f, 285f, false, pFg)
            c.drawText("1980,00", s / 2f, s / 2f + dp(2), pBig)
            c.drawText("GB hoje", s / 2f, s / 2f + dp(18), pSub)
        }
    }

    private fun txt(t: String, s: Float, c: Int, b: Boolean = false) = TextView(this).apply {
        text = t; textSize = s; setTextColor(c)
if (b) setTypeface(null, Typeface.BOLD)
}
}
