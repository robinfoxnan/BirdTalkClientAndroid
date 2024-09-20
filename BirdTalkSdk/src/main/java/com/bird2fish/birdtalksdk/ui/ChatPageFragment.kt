package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bird2fish.birdtalksdk.R

class ChatPageFragment : Fragment() {

    private lateinit var chatId: String
    //private lateinit var chatTextView: TextView

    companion object {
        private const val ARG_CHAT_ID = "chat_id"

        fun newInstance(chatId: String): ChatPageFragment {
            val fragment = ChatPageFragment()
            val args = Bundle()
            args.putString(ARG_CHAT_ID, chatId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取 arguments，注意这一步
        chatId = arguments?.getString(ARG_CHAT_ID) ?: ""  // 安全获取



    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_page, container, false)

       // chatTextView = view.findViewById<TextView>(R.id.chatTextView)


        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d("MyFragment", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("MyFragment", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MyFragment", "onStop called")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("MyFragment", "onDestroyView called")
    }
}
