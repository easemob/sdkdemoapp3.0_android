package com.hyphenate.chatuidemo.conference

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.hyphenate.chat.EMClient
import com.hyphenate.chatuidemo.Constant
import com.hyphenate.chatuidemo.DemoHelper
import com.hyphenate.chatuidemo.R
import com.hyphenate.chatuidemo.ui.BaseActivity
import com.hyphenate.easeui.domain.EaseUser
import com.hyphenate.easeui.utils.EaseUserUtils

/**
 * Created by zhangsong on 18-4-18.
 */
class ConferenceInviteActivity : BaseActivity() {
    companion object {
        val TAG = "ConferenceInvite"

        val STATE_UNCHECKED = 0
        val STATE_CHECKED = 1
        // Already in conference.
        val STATE_CHECKED_UNCHANGEABLE = 2
    }

    private var startBtn: TextView? = null
    private var listView: ListView? = null
    private var contactAdapter: ContactsAdapter? = null
    // Kotlin List is a Read-only list.
    private var contacts: ArrayList<KV<EaseUser, Int>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conference_invite)

        initViews()
        initData()
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btn_cancel -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            R.id.btn_start -> {
                val results = getSelectMembers()

                if (results.isEmpty()) {
                    Toast.makeText(this, getString(R.string.tips_select_contacts_first), Toast.LENGTH_SHORT).show()
                    return
                }

                val i = Intent()
                i.putExtra("members", results)
                setResult(Activity.RESULT_OK, i)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun initViews() {
        val headerView = LayoutInflater.from(this).inflate(R.layout.ease_search_bar, null)
        val query: EditText = headerView.findViewById(R.id.query) as EditText
        val queryClear: ImageView = headerView.findViewById(R.id.search_clear) as ImageView
        query.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                contactAdapter!!.filter(s)
                if (s != null && s.isNotEmpty()) {
                    queryClear.visibility = View.VISIBLE
                } else {
                    queryClear.visibility = View.INVISIBLE
                }
            }
        })
        queryClear.setOnClickListener({
            query.text.clear()
            hideSoftKeyboard()
        })

        startBtn = findViewById(R.id.btn_start) as TextView?
        startBtn!!.text = String.format(getString(R.string.button_start_video_conference), 0)

        contactAdapter = ContactsAdapter(this, contacts)
        contactAdapter!!.checkItemChangeCallback = object : ContactsAdapter.ICheckItemChangeCallback {
            override fun onCheckedItemChanged(v: View, user: EaseUser, state: Int) {
                val count = getSelectMembers().size
                startBtn!!.text = String.format(getString(R.string.button_start_video_conference), count)
            }
        }

        listView = findViewById(R.id.listView) as ListView?
        listView?.addHeaderView(headerView)
        listView?.adapter = contactAdapter
        listView?.setOnTouchListener({ _, _ ->
            hideSoftKeyboard()
            false
        })
    }

    private fun initData() {
        val existMembers = EMClient.getInstance().conferenceManager().conferenceMemberList
        // For test
//        val existMembers = ArrayList<String>()
//        existMembers?.add("js2")

        DemoHelper.getInstance().contactList.values
                .filter {
                    ((it.username != Constant.NEW_FRIENDS_USERNAME)
                            and (it.username != Constant.GROUP_USERNAME)
                            and (it.username != Constant.CHAT_ROOM)
                            and (it.username != Constant.CHAT_ROBOT))
                }
                .forEach {
                    if (existMembers?.contains(it.username) == true) {
                        contacts.add(KV(it, STATE_CHECKED_UNCHANGEABLE))
                    } else {
                        contacts.add(KV(it, STATE_UNCHECKED))
                    }
                }

        contactAdapter?.notifyDataSetChanged(contacts)
    }

    private fun getSelectMembers(): Array<String> {
        val results = ArrayList<String>()
        contacts
                .filter {
                    it.second == STATE_CHECKED
                }
                .forEach {
                    results.add(it.first.username)
                }

        return results.toArray(emptyArray())
    }

    class ContactsAdapter(var context: Context, var list: ArrayList<KV<EaseUser, Int>>) : BaseAdapter() {
        interface ICheckItemChangeCallback {
            fun onCheckedItemChanged(v: View, user: EaseUser, state: Int)
        }

        private var contactFilter: ContactFilter? = null
        private var contacts: ArrayList<KV<EaseUser, Int>> = ArrayList()
        var checkItemChangeCallback: ICheckItemChangeCallback? = null

        companion object {
            val TAG = "ContactsAdapter"
        }

        init {
            contacts.clear()
            contacts.addAll(list)
        }

        fun notifyDataSetChanged(list: ArrayList<KV<EaseUser, Int>>) {
            contacts.clear()
            contacts.addAll(list)
            super.notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var contentView: View? = convertView

            var viewHolder: ViewHolder?
            if (contentView != null) {
                viewHolder = contentView.tag as ViewHolder?
            } else {
                contentView = LayoutInflater.from(context).inflate(R.layout.em_contact_item, null)
                viewHolder = ViewHolder(contentView)
                contentView!!.tag = viewHolder
            }

            viewHolder!!.reset()

            // Handle viewHolder.
            val contact = contacts[position]
            val username = contact.first.username

            EaseUserUtils.setUserAvatar(context, username, viewHolder.headerImage!!)
            EaseUserUtils.setUserNick(username, viewHolder.nameText!!)

            when (contact.second) {
                STATE_CHECKED_UNCHANGEABLE -> {
                    with(viewHolder.checkBox!!) {
                        setButtonDrawable(R.drawable.em_checkbox_bg_gray_selector)
                        isChecked = true
                        // Disable the CheckBox
                        isClickable = false
                    }
                }
                else -> {
                    contentView.setOnClickListener({
                        viewHolder.checkBox?.toggle()
                    })

                    with(viewHolder.checkBox!!) {
                        setButtonDrawable(R.drawable.em_checkbox_bg_selector)
                        isChecked = contact.second == STATE_CHECKED
                        setOnCheckedChangeListener({ _, isChecked ->
                            contact.second = if (isChecked) STATE_CHECKED else STATE_UNCHECKED
                            checkItemChangeCallback?.onCheckedItemChanged(contentView, contact.first, contact.second)
                        })
                    }
                }
            }

            return contentView
        }

        override fun getItem(position: Int): Any {
            return contacts[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return contacts.size
        }

        fun filter(constraint: CharSequence?) {
            if (contactFilter == null) {
                contactFilter = ContactFilter(contacts)
            }

            contactFilter?.filter(
                    constraint,
                    object : ContactFilter.IFilterCallback {
                        override fun onFilter(filtered: List<KV<EaseUser, Int>>) {
                            contacts.clear()
                            contacts.addAll(filtered)
                            if (filtered.isNotEmpty()) {
                                notifyDataSetChanged()
                            } else {
                                notifyDataSetInvalidated()
                            }
                        }
                    }
            )
        }

        class ViewHolder(view: View) {
            var contentView: View? = null
            var headerImage: ImageView? = null
            var nameText: TextView? = null
            var checkBox: CheckBox? = null

            init {
                contentView = view
                headerImage = view.findViewById(R.id.head_icon) as ImageView?
                nameText = view.findViewById(R.id.name) as TextView?
                checkBox = view.findViewById(R.id.checkbox) as CheckBox?
            }

            fun reset() {
                contentView?.setOnClickListener(null)
                nameText?.text = null
                with(checkBox!!) {
                    setOnCheckedChangeListener(null)
                    isChecked = false
                }
            }
        }

        class ContactFilter(list: List<KV<EaseUser, Int>>) : Filter() {
            interface IFilterCallback {
                fun onFilter(filtered: List<KV<EaseUser, Int>>)
            }

            private val contacts: ArrayList<KV<EaseUser, Int>> = ArrayList()
            private var filterCallback: IFilterCallback? = null

            init {
                contacts.clear()
                contacts.addAll(list)
            }

            fun filter(constraint: CharSequence?, callback: IFilterCallback?) {
                filterCallback = callback
                super.filter(constraint)
            }

            override fun performFiltering(prefix: CharSequence?): FilterResults {
                val results = Filter.FilterResults()

                if (prefix == null || prefix.isEmpty()) {
                    results.values = contacts
                    results.count = contacts.size
                } else {
                    val prefixString = prefix.toString()
                    val count = contacts.size
                    val newValues = java.util.ArrayList<KV<EaseUser, Int>>()
                    for (i in 0 until count) {
                        val user = contacts[i]
                        val username = user.first.username

                        if (username.startsWith(prefixString)) {
                            newValues.add(user)
                        } else {
                            val words = username.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                            // Start at index 0, in case valueText starts with space(s)
                            for (word in words) {
                                if (word.startsWith(prefixString)) {
                                    newValues.add(user)
                                    break
                                }
                            }
                        }
                    }
                    results.values = newValues
                    results.count = newValues.size
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filterCallback?.onFilter(results?.values as List<KV<EaseUser, Int>>)
            }
        }
    }

    data class KV<K, V>(var first: K, var second: V)
}