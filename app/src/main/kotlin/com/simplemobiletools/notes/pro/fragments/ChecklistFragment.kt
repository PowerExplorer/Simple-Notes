package com.simplemobiletools.notes.pro.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.notes.pro.R
import com.simplemobiletools.notes.pro.activities.SimpleActivity
import com.simplemobiletools.notes.pro.adapters.ChecklistAdapter
import com.simplemobiletools.notes.pro.dialogs.NewChecklistItemDialog
import com.simplemobiletools.notes.pro.extensions.notesDB
import com.simplemobiletools.notes.pro.helpers.NOTE_ID
import com.simplemobiletools.notes.pro.helpers.NotesHelper
import com.simplemobiletools.notes.pro.models.ChecklistItem
import com.simplemobiletools.notes.pro.models.Note
import kotlinx.android.synthetic.main.fragment_checklist.view.*

class ChecklistFragment : NoteFragment(), RefreshRecyclerViewListener {
    private var noteId = 0L
    private var note: Note? = null
    private var items = ArrayList<ChecklistItem>()

    lateinit var view: ViewGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_checklist, container, false) as ViewGroup
        noteId = arguments!!.getLong(NOTE_ID)
        return view
    }

    override fun onResume() {
        super.onResume()

        NotesHelper(activity!!).getNoteWithId(noteId) {
            if (it != null && activity?.isDestroyed == false) {
                note = it

                val checklistItemType = object : TypeToken<List<ChecklistItem>>() {}.type
                items = Gson().fromJson<ArrayList<ChecklistItem>>(note!!.value, checklistItemType) ?: ArrayList(1)
                setupFragment()
            }
        }
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (menuVisible) {
            activity?.hideKeyboard()
        }
    }

    private fun setupFragment() {
        val plusIcon = resources.getColoredDrawableWithColor(R.drawable.ic_plus, if (context!!.isBlackAndWhiteTheme()) Color.BLACK else Color.WHITE)
        view.checklist_fab.apply {
            setImageDrawable(plusIcon)
            background.applyColorFilter(context!!.getAdjustedPrimaryColor())
            setOnClickListener {
                NewChecklistItemDialog(activity as SimpleActivity) {
                    val currentMaxId = items.maxBy { it.id }?.id ?: 0
                    val checklistItem = ChecklistItem(currentMaxId + 1, it, false)
                    items.add(checklistItem)
                    saveNote(-1)
                }
            }
        }
        setupAdapter()
    }

    private fun setupAdapter() {
        ChecklistAdapter(activity as SimpleActivity, items, this, view.checklist_list) {
            val clickedNote = it as ChecklistItem
            clickedNote.isDone = !clickedNote.isDone
            saveNote(items.indexOfFirst { it.id == clickedNote.id })
        }.apply {
            view.checklist_list.adapter = this
        }
    }

    private fun saveNote(refreshIndex: Int) {
        Thread {
            if (note != null && context != null) {
                if (refreshIndex != -1) {
                    view.checklist_list.post {
                        view.checklist_list.adapter?.notifyItemChanged(refreshIndex)
                    }
                }

                note!!.value = Gson().toJson(items)
                context?.notesDB?.insertOrUpdate(note!!)
            }
        }.start()
    }

    override fun refreshItems() {
    }
}
