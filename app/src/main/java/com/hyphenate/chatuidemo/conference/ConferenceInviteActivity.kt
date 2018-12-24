package com.hyphenate.chatuidemo.conference

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMConferenceMember
import com.hyphenate.chat.EMCursorResult
import com.hyphenate.chatuidemo.Constant
import com.hyphenate.chatuidemo.DemoHelper
import com.hyphenate.chatuidemo.R
import com.hyphenate.chatuidemo.ui.BaseActivity
import com.hyphenate.easeui.utils.EaseUserUtils
import com.hyphenate.exceptions.HyphenateException
import com.hyphenate.util.EMLog
import com.hyphenate.util.EasyUtils

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
    private var contacts: ArrayList<KV<String, Int>> = ArrayList()
    private lateinit var existMembers: List<EMConferenceMember>

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
            override fun onCheckedItemChanged(v: View, username: String, state: Int) {
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
        // Already in conference member list.
        existMembers = EMClient.getInstance().conferenceManager().conferenceMemberList

        val groupId = intent.getStringExtra(Constant.EXTRA_CONFERENCE_GROUP_ID)
        val contactList: ArrayList<String> = ArrayList()

        Thread(Runnable {
            if (TextUtils.isEmpty(groupId)) { // 获取当前账号的所有联系人
                DemoHelper.getInstance().contactList.values
                        .forEach {
                            contactList.add(it.username)
                        }
            } else { // 根据Group-Id获取该群组中所有成员
                contactList.add(EMClient.getInstance().groupManager().getGroupFromServer(groupId).owner)

                var result: EMCursorResult<String>? = null
                do {
                    try {
                        // page size set to 20 is convenient for testing, should be applied to big value
                        result = EMClient.getInstance().groupManager().fetchGroupMembers(groupId,
                                if (result != null) result.cursor else "",
                                20)
                    } catch (e: HyphenateException) {
                        e.printStackTrace()
                    }
                    EMLog.d(TAG, "fetchGroupMembers result.size:" + result?.data?.size)
                    if (result != null) {
                        contactList.addAll(result.data)
                    }
                } while (result!!.cursor != null && !result.cursor.isEmpty())
            }

            runOnUiThread {
                contactList.
                        filter {
                            ((it != Constant.NEW_FRIENDS_USERNAME)
                                    and (it != Constant.GROUP_USERNAME)
                                    and (it != Constant.CHAT_ROOM)
                                    and (it != Constant.CHAT_ROBOT)
                                    and (it != EMClient.getInstance().currentUser))
                        }
                        .forEach {
                            if (memberContains(it) != null) {
                                contacts.add(KV(it, STATE_CHECKED_UNCHANGEABLE))
                            } else {
                                contacts.add(KV(it, STATE_UNCHECKED))
                            }
                        }

                // 将对contacts变量的处理和contactAdapter#notifyDataSetChanged()放在同一线程,否则有几率出现以下错误:
                // IllegalStateException: The content of the adapter has changed but ListView did not receive a notification.
                contactAdapter?.notifyDataSetChanged()
            }
        }).start()
    }

    private fun getSelectMembers(): Array<String> {
        val results = ArrayList<String>()
        contacts
                .filter {
                    it.second == STATE_CHECKED
                }
                .forEach {
                    results.add(it.first)
                }

        return results.toArray(emptyArray())
    }

    private fun memberContains(name: String): EMConferenceMember? {
        for (item in existMembers) {
            if (EasyUtils.useridFromJid(item.memberName) == name) {
                return item
            }
        }
        return null
    }

    class ContactsAdapter(var context: Context, private var contacts: ArrayList<KV<String, Int>>) : BaseAdapter() {
        interface ICheckItemChangeCallback {
            fun onCheckedItemChanged(v: View, username: String, state: Int)
        }

        var checkItemChangeCallback: ICheckItemChangeCallback? = null
        private var contactFilter: ContactFilter? = null
        private val filteredContacts = ArrayList<KV<String, Int>>()

        companion object {
            val TAG = "ContactsAdapter"
        }

        init {
            filteredContacts.addAll(contacts)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var contentView: View? = convertView

            val viewHolder: ViewHolder?
            if (contentView != null) {
                viewHolder = contentView.tag as ViewHolder?
            } else {
                contentView = LayoutInflater.from(context).inflate(R.layout.em_contact_item, null)
                viewHolder = ViewHolder(contentView)
                contentView!!.tag = viewHolder
            }

            viewHolder!!.reset()

            // Handle viewHolder.
            val contact = filteredContacts[position]
            val username = contact.first

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
            return filteredContacts[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return filteredContacts.size
        }

        override fun notifyDataSetChanged() {
            filteredContacts.clear()
            filteredContacts.addAll(contacts)
            notifyActual()
        }

        private fun notifyActual() {
            super.notifyDataSetChanged()
        }

        fun filter(constraint: CharSequence?) {
            if (contactFilter == null) {
                contactFilter = ContactFilter(contacts)
            }

            contactFilter?.filter(
                    constraint,
                    object : ContactFilter.IFilterCallback {
                        override fun onFilter(filtered: List<KV<String, Int>>) {
                            filteredContacts.clear()
                            filteredContacts.addAll(filtered)
                            if (filtered.isNotEmpty()) {
                                notifyActual()
                            } else {
                                notifyDataSetInvalidated()
                            }
                        }
                    }
            )
        }

        class ViewHolder(var view: View) {
            var headerImage: ImageView? = null
            var nameText: TextView? = null
            var checkBox: CheckBox? = null

            init {
                headerImage = view.findViewById(R.id.head_icon) as ImageView?
                nameText = view.findViewById(R.id.name) as TextView?
                checkBox = view.findViewById(R.id.checkbox) as CheckBox?
            }

            fun reset() {
                view.setOnClickListener(null)
                nameText?.text = null
                with(checkBox!!) {
                    setOnCheckedChangeListener(null)
                    isChecked = false
                }
            }
        }

        class ContactFilter(private val contacts: List<KV<String, Int>>) : Filter() {
            interface IFilterCallback {
                fun onFilter(filtered: List<KV<String, Int>>)
            }

            private var filterCallback: IFilterCallback? = null


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
                    val newValues = java.util.ArrayList<KV<String, Int>>()
                    for (i in 0 until count) {
                        val user = contacts[i]
                        val username = user.first

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
                val result = if (results!!.values != null) results!!.values as List<KV<String, Int>> else emptyList()
                filterCallback?.onFilter(result)
            }
        }
    }

    data class KV<K, V>(var first: K, var second: V)
}